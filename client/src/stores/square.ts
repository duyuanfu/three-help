import { defineStore } from 'pinia';
import type { PostItem, CommentItem } from '../types';
import { TABLES } from '../utils/db';
import { useUserStore } from './user';
import {
  apiAddComment,
  apiCreatePost,
  apiDeletePost,
  apiListComments,
  apiToggleLike
} from '../api/business';

export const useSquareStore = defineStore('square', {
  state: () => ({
    posts: [] as PostItem[],
    comments: [] as CommentItem[],
    searchKeyword: ''
  }),
  getters: {
    filteredPosts: (state) => {
      if (!state.searchKeyword.trim()) return state.posts;
      const kw = state.searchKeyword.trim().toLowerCase();
      return state.posts.filter(
        (p) => p.authorName.toLowerCase().includes(kw) || p.content.toLowerCase().includes(kw)
      );
    },
    myPosts: (state) => {
      const currentUserId = useUserStore().currentUser.id;
      return state.posts.filter((p) => p.authorId === currentUserId);
    },
    // 指定帖子的真实评论列表
    commentsByPost: (state) => {
      return (postId: string) => state.comments.filter((c) => c.postId === postId);
    },
    // 指定帖子的真实评论数
    commentCountOf: (state) => {
      return (postId: string) => state.comments.filter((c) => c.postId === postId).length;
    }
  },
  actions: {
    /** 启动时从后端快照读取（动态与评论是全局公开数据） */
    initSquare() {
      try {
        this.posts = TABLES.posts.all();
        this.comments = TABLES.comments.all();
        this.syncCommentCounts();
      } catch (e) {
        console.error('[square] 初始化失败', e);
      }
    },

    /** 拉取某个帖子的评论（进入评论页时调用） */
    async loadComments(postId: string) {
      const list = await apiListComments(postId);
      this.comments = [
        ...this.comments.filter((c) => c.postId !== postId),
        ...list
      ];
      const post = this.posts.find((p) => p.id === postId);
      if (post) {
        post.commentCount = list.length;
      }
      return list;
    },

    // 让帖子上的 commentCount 与实际评论列表保持一致
    syncCommentCounts() {
      this.posts.forEach((post) => {
        const realCount = this.comments.filter((c) => c.postId === post.id).length;
        if (post.commentCount !== realCount) {
          post.commentCount = realCount;
        }
      });
    },

    /** 发表评论：评论者由服务端按令牌确定 */
    async addComment(postId: string, content: string) {
      const saved = await apiAddComment(postId, content);
      this.comments.push(saved);
      const post = this.posts.find((p) => p.id === postId);
      if (post) {
        post.commentCount = this.comments.filter((c) => c.postId === postId).length;
      }
      return saved;
    },

    /** 删除动态（含其评论），仅作者本人可删 */
    async deletePost(postId: string) {
      await apiDeletePost(postId);
      this.posts = this.posts.filter((p) => p.id !== postId);
      this.comments = this.comments.filter((c) => c.postId !== postId);
    },

    /** 点赞 / 取消点赞 */
    async toggleLike(postId: string) {
      try {
        const saved = await apiToggleLike(postId);
        const post = this.posts.find((p) => p.id === postId);
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
      this.posts.unshift(post);
      return post;
    }
  }
});
