import { defineStore } from 'pinia';
import type { PostItem } from '../types';
import { initialPosts } from '../mock/seeds';

const POSTS_KEY = 'bbb_posts';

export const useSquareStore = defineStore('square', {
  state: () => ({
    posts: [] as PostItem[],
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
      return state.posts.filter((p) => p.authorId === 'u_zhangdongmei');
    }
  },
  actions: {
    initSquare() {
      try {
        const cached = uni.getStorageSync(POSTS_KEY);
        if (Array.isArray(cached) && cached.length > 0) {
          this.posts = cached;
        } else {
          this.posts = [...initialPosts];
          this.savePosts();
        }
      } catch (e) {
        console.error('Failed to init square', e);
      }
    },
    savePosts() {
      try {
        uni.setStorageSync(POSTS_KEY, this.posts);
      } catch (e) {
        console.error('Failed to save posts', e);
      }
    },
    createPost(content: string, images?: string[]) {
      const newPost: PostItem = {
        id: 'post_' + Date.now(),
        authorId: 'u_zhangdongmei',
        authorName: '张冬梅',
        authorAvatar: '/static/avatars/zhangdongmei.png',
        content,
        images: images || [],
        likeCount: 0,
        commentCount: 0,
        timeAgo: '刚刚',
        isLiked: false
      };
      this.posts.unshift(newPost);
      this.savePosts();
      return newPost;
    },
    toggleLike(postId: string) {
      const post = this.posts.find((p) => p.id === postId);
      if (post) {
        post.isLiked = !post.isLiked;
        post.likeCount += post.isLiked ? 1 : -1;
        this.savePosts();
      }
    },
    deletePost(postId: string) {
      this.posts = this.posts.filter((p) => p.id !== postId);
      this.savePosts();
    }
  }
});
