import { defineStore } from 'pinia';
import type { PostItem, CommentItem } from '../types';
import { initialPosts, initialComments } from '../mock/seeds';
import { TABLES } from '../utils/db';
import { useUserStore } from './user';

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
    initSquare() {
      try {
        this.posts = TABLES.posts.all([...initialPosts]);
        this.comments = TABLES.comments.all([...initialComments]);
        // 同步帖子上的评论数字段与真实评论数
        this.syncCommentCounts();
      } catch (e) {
        console.error('Failed to init square', e);
      }
    },
    savePosts() {
      TABLES.posts.replaceAll(this.posts);
    },
    saveComments() {
      TABLES.comments.replaceAll(this.comments);
    },
    // 让帖子上的 commentCount 与实际评论列表保持一致
    syncCommentCounts() {
      this.posts.forEach((post) => {
        post.commentCount = this.comments.filter((c) => c.postId === post.id).length;
      });
      this.savePosts();
    },
    // 新增评论（评论页后续接入输入时使用）
    addComment(postId: string, userName: string, avatar: string, content: string) {
      const newComment: CommentItem = {
        id: 'cmt_' + Date.now(),
        postId,
        userName,
        avatar,
        content
      };
      this.comments.push(newComment);
      TABLES.comments.insert(newComment, 'tail');
      this.syncCommentCounts();
      return newComment;
    },
    // 删除帖子时同步清理其评论
    deletePost(postId: string) {
      this.posts = this.posts.filter((p) => p.id !== postId);
      TABLES.posts.remove(postId);
      this.comments = this.comments.filter((c) => c.postId !== postId);
      TABLES.comments.remove((c: CommentItem) => c.postId === postId);
      this.saveComments();
    },
    toggleLike(postId: string) {
      const post = this.posts.find((p) => p.id === postId);
      if (post) {
        post.isLiked = !post.isLiked;
        post.likeCount += post.isLiked ? 1 : -1;
        this.savePosts();
      }
    },
    createPost(content: string, images?: string[]) {
      const user = useUserStore().currentUser;
      const newPost: PostItem = {
        id: 'post_' + Date.now(),
        authorId: user.id,
        authorName: user.name,
        authorAvatar: user.avatar,
        content,
        images: images || [],
        likeCount: 0,
        commentCount: 0,
        timeAgo: '刚刚',
        isLiked: false
      };
      this.posts.unshift(newPost);
      TABLES.posts.insert(newPost, 'head');
      return newPost;
    }
  }
});
