import { defineStore } from 'pinia';
import type { ContactItem, ChatSession, ChatMessage, MessageType } from '../types';
import { TABLES } from '../utils/db';
import {
  apiAddContact,
  apiDeleteContact,
  apiEnsureSession,
  apiListContacts,
  apiListMessages,
  apiMarkSessionRead,
  apiSendMessage
} from '../api/business';
import type { SessionView } from '../api/business';

/**
 * 私信内容不保留标点：去掉「，。？！」及对应的英文标点。
 *
 * 注意英文句点 `.` 不在清理范围内 —— 它同时是小数点、域名、版本号的分隔符，
 * 无差别删掉会把「3.5 元」变成「35 元」、把网址拼坏。
 */
export const stripPunctuation = (text: string): string => {
  if (!text) return '';
  return text.replace(/[，。？！,?!]/g, '').trim();
};

/** 会话视图 → 前端 ChatSession */
const toChatSession = (view: SessionView): ChatSession => ({
  sessionId: view.sessionId,
  targetUser: {
    id: view.targetUser ? view.targetUser.id : '',
    name: view.targetUser ? view.targetUser.name : '未知联系人',
    avatar: view.targetUser ? view.targetUser.avatar : '',
    tag: view.targetUser ? view.targetUser.tag : ''
  },
  lastMessage: view.lastMessage || '',
  lastTime: view.lastTime || '',
  unreadCount: view.unreadCount || 0,
  relatedOrderId: undefined
});

export const useChatStore = defineStore('chat', {
  state: () => ({
    contacts: [] as ContactItem[],
    sessions: [] as ChatSession[],
    messages: {} as Record<string, ChatMessage[]>,
    currentSessionId: '',
    searchKeyword: ''
  }),
  getters: {
    // 按首字母 A-Z 分组排序的联系人
    groupedContacts: (state) => {
      let filtered = state.contacts;
      if (state.searchKeyword.trim()) {
        const kw = state.searchKeyword.trim().toLowerCase();
        filtered = filtered.filter((c) => c.name.toLowerCase().includes(kw));
      }
      const groups: Record<string, ContactItem[]> = {};
      filtered.forEach((c) => {
        const letter = (c.letter || 'A').toUpperCase();
        if (!groups[letter]) {
          groups[letter] = [];
        }
        groups[letter].push(c);
      });
      const sortedKeys = Object.keys(groups).sort();
      return sortedKeys.map((letter) => ({
        letter,
        list: groups[letter]
      }));
    },
    // 过滤后的私信会话列表
    filteredSessions: (state) => {
      if (!state.searchKeyword.trim()) return state.sessions;
      const kw = state.searchKeyword.trim().toLowerCase();
      return state.sessions.filter(
        (s) => s.targetUser.name.toLowerCase().includes(kw) || s.lastMessage.toLowerCase().includes(kw)
      );
    },
    currentMessages: (state) => {
      return state.messages[state.currentSessionId] || [];
    },
    totalUnread: (state) => state.sessions.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
  },
  actions: {
    /** 启动时从后端快照读取（bootstrap 已按当前用户过滤） */
    initChat() {
      try {
        this.contacts = TABLES.contacts.all();
        this.sessions = TABLES.sessions.all();
        this.messages = TABLES.messages.all();
      } catch (e) {
        console.error('[chat] 初始化失败', e);
      }
    },

    /** 重新拉取通讯录与会话 */
    async reloadChat() {
      const [contacts, sessionsRaw] = await Promise.all([apiListContacts(), Promise.resolve(TABLES.sessions.all())]);
      this.contacts = contacts;
      this.sessions = sessionsRaw;
    },

    /** 新增联系人：按手机号或对方账号ID关联真实用户 */
    async addContact(contact: { name?: string; phone?: string; tag?: string; avatar?: string; targetUserId?: string }) {
      const saved = await apiAddContact({
        name: contact.name,
        phone: contact.phone,
        tag: contact.tag,
        avatar: contact.avatar,
        targetUserId: contact.targetUserId
      });
      const exists = this.contacts.find((c) => c.id === saved.id);
      if (!exists) {
        this.contacts.push(saved);
      }
      return saved;
    },

    async removeContact(id: string) {
      await apiDeleteContact(id);
      this.contacts = this.contacts.filter((c) => c.id !== id);
    },

    /** 打开或创建与某人的会话（服务端保证同一对用户只有一条会话） */
    async openOrCreateSession(targetUserId: string) {
      const view = await apiEnsureSession(targetUserId);
      const session = toChatSession(view);
      const idx = this.sessions.findIndex((s) => s.sessionId === session.sessionId);
      if (idx === -1) {
        this.sessions.unshift(session);
      } else {
        this.sessions[idx] = { ...this.sessions[idx], ...session };
      }
      this.currentSessionId = session.sessionId;
      if (!this.messages[session.sessionId]) {
        this.messages[session.sessionId] = [];
      }
      return session;
    },

    /** 拉取某个会话的消息（进入聊天页 + 轮询时调用） */
    async loadMessages(sessionId: string) {
      const list = await apiListMessages(sessionId);
      this.messages[sessionId] = list;
      return list;
    },

    /** 标记已读 */
    async markRead(sessionId: string) {
      const session = this.sessions.find((s) => s.sessionId === sessionId);
      if (session) {
        session.unreadCount = 0;
      }
      try {
        await apiMarkSessionRead(sessionId);
      } catch (e) {
        // 已读失败不影响使用
      }
    },

    /** 发送消息：发送者由服务端按令牌确定 */
    async sendMessage(params: {
      type: MessageType;
      content: string;
      sessionId?: string;
      duration?: number;
      audioPath?: string;
    }) {
      const sid = params.sessionId || this.currentSessionId;
      if (!sid) {
        throw new Error('请先选择聊天对象');
      }

      // 私信不保留标点「，。？！」（服务端也会再清理一次）
      const content = params.type === 'text' ? stripPunctuation(params.content) : params.content;
      if (params.type === 'text' && !content) {
        throw new Error('消息内容不能全是标点符号');
      }

      const msg = await apiSendMessage({
        sessionId: sid,
        type: params.type,
        content,
        duration: params.duration,
        audioPath: params.audioPath
      });

      if (!this.messages[sid]) {
        this.messages[sid] = [];
      }
      // 防止轮询已插入同一条
      if (!this.messages[sid].some((m) => m.id === msg.id)) {
        this.messages[sid].push(msg);
      }

      const session = this.sessions.find((s) => s.sessionId === sid);
      if (session) {
        session.lastMessage =
          params.type === 'text'
            ? content
            : params.type === 'voice'
              ? '[语音]'
              : `[${params.type}]`;
        session.lastTime = msg.createdAt;
      }
      return msg;
    },

    getFirstLetter(char: string) {
      const map: Record<string, string> = {
        李: 'L',
        马: 'M',
        王: 'W',
        张: 'Z',
        刘: 'L',
        吴: 'W',
        杨: 'Y',
        蓝: 'L',
        陈: 'C',
        赵: 'Z',
        孙: 'S',
        周: 'Z'
      };
      return map[char] || 'L';
    }
  }
});
