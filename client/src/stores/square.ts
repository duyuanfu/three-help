import { defineStore } from 'pinia';
import type { PostItem, CommentItem } from '../types';
import {
  apiAddComment,
  apiCreatePost,
  apiDeletePost,
  apiGetPost,
  apiListComments,
  apiMyPosts,
  apiPostFeed,
  apiToggleLike
} from '../api/business';

/** 每页条数（服务端会再夹到 1~50） */
const PAGE_SIZE = 20;

/** 追加时按 id 去重：翻页期间自己刚发的、或被别人顶到前面的不会重复 */
const mergeById = (base: PostItem[], extra: PostItem[]): PostItem[] => {
  const seen = new Set(base.map((post) => post.id));
  const result = base.slice();
  extra.forEach((post) => {
    if (post && !seen.has(post.id)) {
      seen.add(post.id);
      result.push(post);
    }
  });
  return result;
};

/**
 * 广场数据
 *
 * 动态与评论不在启动快照里（它们会无限增长），全部走接口：
 *   - 广场列表：/api/posts/feed 游标分页，滚动到底加载下一页
 *   - 我的动态：/api/posts/mine 独立分页，不受广场翻到第几页影响
 *   - 评论：进入评论页时按 postId 全量拉取
 */
export const useSquareStore = defineStore('square', {
  state: () => ({
    /** 广场已加载的动态（新 → 旧） */
    posts: [] as PostItem[],
    /** 已加载的评论（按 postId 平铺，页面自行过滤） */
    comments: [] as CommentItem[],
    searchKeyword: '',

    feedCursor: null as number | null,
    feedHasMore: true,
    feedLoading: false,
    feedLoaded: false,

    /** 我的动态（独立分页） */
    myPosts: [] as PostItem[],
    myCursor: null as number | null,
    myHasMore: true,
    myLoading: false,
    myLoaded: false
  }),

  getters: {
    /**
     * 服务端已按关键字过滤，这里再本地过一遍只是为了让输入时立刻有反馈；
     * 它只会把结果变窄，不会把服务端返回的条目丢掉。
     */
    filteredPosts: (state) => {
      const kw = state.searchKeyword.trim().toLowerCase();
      if (!kw) return state.posts;
      return state.posts.filter(
        (p) =>
          (p.authorName || '').toLowerCase().includes(kw) ||
          (p.content || '').toLowerCase().includes(kw)
      );
    },

    /** 指定帖子的评论列表 */
    commentsByPost: (state) => {
      return (postId: string) => state.comments.filter((c) => c.postId === postId);
    }
  },

  actions: {
    /** 退出登录 / 换账号时清空，避免看到上一个账号的数据 */
    resetSquare() {
      this.posts = [];
      this.comments = [];
      this.searchKeyword = '';
      this.feedCursor = null;
      this.feedHasMore = true;
      this.feedLoading = false;
      this.feedLoaded = false;
      this.myPosts = [];
      this.myCursor = null;
      this.myHasMore = true;
      this.myLoading = false;
      this.myLoaded = false;
    },

    /** 加载广场动态；reset=true 表示回到第一页（首次进入、下拉刷新、换搜索词） */
    async loadFeed(reset = false) {
      if (this.feedLoading) return;
      if (!reset && !this.feedHasMore) return;

      this.feedLoading = true;
      try {
        const page = await apiPostFeed({
          cursor: reset ? null : this.feedCursor,
          // 关键字要一直带着：翻到第 2 页时服务端同样需要它来过滤
          keyword: this.searchKeyword.trim() || undefined,
          limit: PAGE_SIZE
        });
        this.posts = reset ? page.items : mergeById(this.posts, page.items);
        this.feedCursor = page.nextCursor;
        this.feedHasMore = page.hasMore;
        this.feedLoaded = true;
      } catch (e) {
        console.error('[square] 动态加载失败', e);
        // 首屏失败要让调用方知道；翻页失败只提示，已加载的内容保持不动
        if (reset) throw e;
      } finally {
        this.feedLoading = false;
      }
    },

    /** 滚动到底：没有更多时直接返回，不重复请求 */
    async loadMoreFeed() {
      if (this.feedLoading || !this.feedHasMore || !this.feedLoaded) return;
      await this.loadFeed(false);
    },

    /** 加载「我的动态」第一页或下一页 */
    async loadMyPosts(reset = false) {
      if (this.myLoading) return;
      if (!reset && !this.myHasMore) return;

      this.myLoading = true;
      try {
        const page = await apiMyPosts({
          cursor: reset ? null : this.myCursor,
          limit: PAGE_SIZE
        });
        this.myPosts = reset ? page.items : mergeById(this.myPosts, page.items);
        this.myCursor = page.nextCursor;
        this.myHasMore = page.hasMore;
        this.myLoaded = true;
      } catch (e) {
        console.error('[square] 我的动态加载失败', e);
        if (reset) throw e;
      } finally {
        this.myLoading = false;
      }
    },

    async loadMoreMyPosts() {
      if (this.myLoading || !this.myHasMore || !this.myLoaded) return;
      await this.loadMyPosts(false);
    },

    /** 单个帖子详情：评论页可能是从「我的动态」进来的，列表里不一定有这条 */
    async fetchPost(postId: string): Promise<PostItem | null> {
      const cached =
        this.posts.find((p) => p.id === postId) || this.myPosts.find((p) => p.id === postId);
      if (cached) return cached;
      try {
        return await apiGetPost(postId);
      } catch (e) {
        console.error('[square] 动态详情加载失败', e);
        return null;
      }
    },

    /** 拉取某个帖子的评论（进入评论页时调用，服务端返回该帖全部评论） */
    async loadComments(postId: string) {
      const list = await apiListComments(postId);
      this.comments = [...this.comments.filter((c) => c.postId !== postId), ...list];
      const post = this.findPost(postId);
      if (post) {
        post.commentCount = list.length;
      }
      return list;
    },

    /** 发表评论：评论者由服务端按令牌确定 */
    async addComment(postId: string, content: string) {
      const saved = await apiAddComment(postId, content);
      if (!this.comments.some((c) => c.id === saved.id)) {
        this.comments.push(saved);
      }
      const post = this.findPost(postId);
      if (post) {
        // 本地自增，避免为了刷新一个数字再拉一次列表
        post.commentCount = (post.commentCount || 0) + 1;
      }
      return saved;
    },

    /** 删除动态（含其评论），仅作者本人可删 */
    async deletePost(postId: string) {
      await apiDeletePost(postId);
      this.posts = this.posts.filter((p) => p.id !== postId);
      this.myPosts = this.myPosts.filter((p) => p.id !== postId);
      this.comments = this.comments.filter((c) => c.postId !== postId);
    },

    /** 点赞 / 取消点赞 */
    async toggleLike(postId: string) {
      try {
        const saved = await apiToggleLike(postId);
        const post = this.findPost(postId);
        if (post) {
          post.isLiked = saved.isLiked;
          post.likeCount = saved.likeCount;
        }
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none', duration: 1800 });
      }
    },

    /** 发布动态：作者由服务端按令牌确定，图片是上传后的服务器地址 */
    async createPost(content: string, images?: string[]) {
      const post = await apiCreatePost(content, images || []);
      // 插到最前面即可：新帖的 sort_no 最小，正好是列表顺序的第一条
      if (!this.posts.some((p) => p.id === post.id)) {
        this.posts.unshift(post);
      }
      if (this.myLoaded && !this.myPosts.some((p) => p.id === post.id)) {
        this.myPosts.unshift(post);
      }
      return post;
    },

    /** 两个列表里找同一条，点赞/评论数要一起更新 */
    findPost(postId: string): PostItem | undefined {
      return this.posts.find((p) => p.id === postId) || this.myPosts.find((p) => p.id === postId);
    }
  }
});
