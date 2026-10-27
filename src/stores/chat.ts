import { defineStore } from 'pinia';
import type { ContactItem, ChatSession, ChatMessage, MessageType } from '../types';
import { initialContacts, initialSessions, initialChatMessages } from '../mock/seeds';

const CONTACTS_KEY = 'bbb_contacts';
const SESSIONS_KEY = 'bbb_sessions';
const MESSAGES_KEY = 'bbb_messages';

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
    }
  },
  actions: {
    initChat() {
      try {
        const cachedContacts = uni.getStorageSync(CONTACTS_KEY);
        this.contacts = Array.isArray(cachedContacts) && cachedContacts.length > 0 
          ? cachedContacts 
          : [...initialContacts];

        const cachedSessions = uni.getStorageSync(SESSIONS_KEY);
        this.sessions = Array.isArray(cachedSessions) && cachedSessions.length > 0 
          ? cachedSessions 
          : [...initialSessions];

        const cachedMessages = uni.getStorageSync(MESSAGES_KEY);
        this.messages = cachedMessages && typeof cachedMessages === 'object' 
          ? cachedMessages 
          : { ...initialChatMessages };

        this.saveAll();
      } catch (e) {
        console.error('Failed to init chat', e);
      }
    },
    saveAll() {
      try {
        uni.setStorageSync(CONTACTS_KEY, this.contacts);
        uni.setStorageSync(SESSIONS_KEY, this.sessions);
        uni.setStorageSync(MESSAGES_KEY, this.messages);
      } catch (e) {
        console.error('Failed to save chat data', e);
      }
    },
    // 新增联系人
    addContact(contact: { name: string; phone: string; tag?: string }) {
      // 简单拼音首字母估算
      const firstChar = contact.name.trim()[0] || 'A';
      const letter = this.getFirstLetter(firstChar);
      const newContact: ContactItem = {
        id: 'c_' + Date.now(),
        name: contact.name,
        avatar: '/static/avatars/zhangdongmei.png',
        tag: contact.tag,
        letter,
        phone: contact.phone,
        isFriend: true
      };
      this.contacts.push(newContact);
      this.saveAll();
      return newContact;
    },
    // 打开或创建私信会话
    openOrCreateSession(targetUser: { id: string; name: string; avatar: string; tag?: string }) {
      let session = this.sessions.find((s) => s.targetUser.id === targetUser.id);
      if (!session) {
        const newSessionId = 'sess_' + targetUser.id;
        session = {
          sessionId: newSessionId,
          targetUser,
          lastMessage: '新建立了私信沟通',
          lastTime: '刚刚',
          unreadCount: 0
        };
        this.sessions.unshift(session);
        if (!this.messages[newSessionId]) {
          this.messages[newSessionId] = [];
        }
        this.saveAll();
      }
      this.currentSessionId = session.sessionId;
      return session;
    },
    // 发送消息
    sendMessage(params: {
      type: MessageType;
      content: string;
      sessionId?: string;
      duration?: number;
      audioPath?: string;
    }) {
      const sid = params.sessionId || this.currentSessionId;
      if (!sid) return;

      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')} : ${String(now.getMinutes()).padStart(2, '0')}`;

      const msg: ChatMessage = {
        id: 'msg_' + Date.now(),
        sessionId: sid,
        senderId: 'u_zhangdongmei',
        receiverId: 'target',
        type: params.type,
        content: params.content,
        duration: params.duration,
        audioPath: params.audioPath,
        createdAt: timeStr,
        timestamp: Date.now()
      };

      if (!this.messages[sid]) {
        this.messages[sid] = [];
      }
      this.messages[sid].push(msg);

      // 更新会话的最后一条消息
      const session = this.sessions.find((s) => s.sessionId === sid);
      if (session) {
        if (params.type === 'text') {
          session.lastMessage = params.content;
        } else if (params.type === 'voice') {
          session.lastMessage = '[语音]';
        } else {
          session.lastMessage = `[${params.type}]`;
        }
        session.lastTime = timeStr;
      }

      this.saveAll();

      // 模拟 1.2 秒后对方自动回复
      setTimeout(() => {
        this.simulateReply(sid);
      }, 1200);

      return msg;
    },
    // 模拟对方回复
    simulateReply(sid: string) {
      const session = this.sessions.find((s) => s.sessionId === sid);
      const name = session ? session.targetUser.name : '对方';
      const replies = [
        '收到啦，张阿姨您放心！',
        '好嘞，我这就记下来。',
        '没问题，有事您随时喊我！',
        '好啊，先这样约好。'
      ];
      const randomReply = replies[Math.floor(Math.random() * replies.length)];
      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')} : ${String(now.getMinutes()).padStart(2, '0')}`;

      const replyMsg: ChatMessage = {
        id: 'msg_reply_' + Date.now(),
        sessionId: sid,
        senderId: session ? session.targetUser.id : 'target',
        receiverId: 'u_zhangdongmei',
        type: 'text',
        content: randomReply,
        createdAt: timeStr,
        timestamp: Date.now()
      };

      if (!this.messages[sid]) {
        this.messages[sid] = [];
      }
      this.messages[sid].push(replyMsg);

      if (session) {
        session.lastMessage = randomReply;
        session.lastTime = timeStr;
      }
      this.saveAll();
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
