import type { 
  UserProfile, 
  HelpOrder, 
  ContactItem, 
  ChatMessage, 
  ChatSession, 
  PostItem, 
  MallGoods, 
  ExchangeOrder 
} from '../types';

export const initialUser: UserProfile = {
  id: 'u_zhangdongmei',
  name: '张冬梅',
  avatar: '/static/avatars/zhangdongmei.png',
  gender: '女',
  age: 65,
  phone: '138****5920',
  isRealName: true,
  identityTag: '',
  occupation: '退休教师',
  address: '福建省泉州市丰泽区凤阳小区8号楼302',
  organization: '泉州市丰泽区东湖街道社区居委会',
  points: 1000
};

export const initialOrders: HelpOrder[] = [
  {
    id: 'ord_001',
    creatorId: 'u_zhangdongmei',
    creatorName: '张冬梅',
    creatorAvatar: '/static/avatars/zhangdongmei.png',
    category: 'medical',
    title: '医院陪诊',
    content: '泉州市第一医院骨科门诊陪同挂号取药与照料',
    appointTime: '2025/09/03 8:00 — 12:00',
    address: '泉州市第一医院东街院区',
    bounty: 50,
    status: 'in_progress',
    statusText: '帮手即将到达',
    helper: {
      id: 'u_wuxiaoying',
      name: '吴小应',
      tag: '@学生',
      avatar: '/static/avatars/malingling.png',
      phone: '13912345678'
    },
    createdAt: '2025/09/02 18:00'
  },
  {
    id: 'ord_002',
    creatorId: 'u_zhangdongmei',
    creatorName: '张冬梅',
    creatorAvatar: '/static/avatars/zhangdongmei.png',
    category: 'delivery',
    title: '帮抢鸡蛋',
    content: '永辉超市早市特价土鸡蛋抢购代买送至家门',
    appointTime: '2025/09/03 17:30 — 18:00',
    address: '凤阳小区8号楼302',
    bounty: 20,
    status: 'in_progress',
    statusText: '已匹配到 刘小燕 帮手',
    helper: {
      id: 'u_liuxiaoyan',
      name: '刘小燕',
      tag: '@学生',
      avatar: '/static/avatars/malingling.png',
      phone: '13987654321'
    },
    createdAt: '2025/09/03 10:20'
  },
  {
    id: 'ord_003',
    creatorId: 'u_zhangdongmei',
    creatorName: '张冬梅',
    creatorAvatar: '/static/avatars/zhangdongmei.png',
    category: 'teach_phone',
    title: '教会手机',
    content: '教老人使用微信视频通话、线上生活缴费与挂号操作',
    appointTime: '2025/08/25 14:30 — 18:00',
    address: '凤阳小区8号楼302',
    bounty: 35,
    status: 'completed',
    statusText: '已完成服务',
    helper: {
      id: 'u_languiping',
      name: '蓝贵萍',
      tag: '@村支书',
      avatar: '/static/avatars/wangqiuhua.png',
      phone: '13866668888'
    },
    createdAt: '2025/08/25 10:00',
    completedAt: '2025/08/25 18:00'
  },
  {
    id: 'ord_004',
    creatorId: 'u_wangnainai',
    creatorName: '王奶奶',
    creatorAvatar: '/static/avatars/wangnainai.png',
    category: 'shopping',
    title: '陪散心',
    content: '需要有人来陪散心',
    appointTime: '2026/06/08 14:00-17:00',
    address: '凤阳小区8号楼',
    bounty: 30,
    status: 'pending',
    statusText: '等待爱心帮手接单',
    createdAt: '2026/06/08 09:30'
  }
];

export const initialExchangeOrders: ExchangeOrder[] = [
  {
    id: 'ex_001',
    orderNo: '75124398600122',
    goodsName: '兑换鸡蛋一盒',
    goodsImage: '/static/goods/egg.jpg',
    quantity: 1,
    pointsCost: 1000,
    address: '福建省泉州市丰泽区凤阳小区8号楼302',
    expressCompany: '中通快递',
    senderCity: '泉州市 帮帮超市',
    receiverCity: '泉州市 张冬梅',
    createdAt: '2025/08/23 18:28',
    steps: [
      {
        time: '2025/08/23 20:00',
        title: '已发货',
        subtitle: '泉州分拨中心揽收',
        isCurrent: false
      },
      {
        time: '2025/08/24 8:00',
        title: '转站中',
        subtitle: '华大接驳点',
        isCurrent: false
      },
      {
        time: '2025/08/24 12:00',
        title: '已到达',
        subtitle: '丰泽区服务站',
        code: '取件码 70-2-0029',
        isCurrent: true
      }
    ]
  }
];

export const initialContacts: ContactItem[] = [
  {
    id: 'c_li',
    name: '李老头',
    avatar: '/static/avatars/lilaotou.png',
    letter: 'L',
    phone: '13800000001',
    isFriend: true
  },
  {
    id: 'c_ma',
    name: '马玲玲',
    avatar: '/static/avatars/malingling.png',
    tag: '@学生',
    letter: 'M',
    phone: '13900000002',
    isFriend: true
  },
  {
    id: 'c_wang',
    name: '王秋华',
    avatar: '/static/avatars/wangqiuhua.png',
    tag: '@村委会干事',
    letter: 'W',
    phone: '13700000003',
    isFriend: true
  }
];

export const initialSessions: ChatSession[] = [
  {
    sessionId: 'sess_li',
    targetUser: {
      id: 'c_li',
      name: '李老头',
      avatar: '/static/avatars/lilaotou.png'
    },
    lastMessage: '好啊，先这样约好',
    lastTime: '19 : 58',
    unreadCount: 0
  },
  {
    sessionId: 'sess_ma',
    targetUser: {
      id: 'c_ma',
      name: '马玲玲',
      avatar: '/static/avatars/malingling.png',
      tag: '@学生'
    },
    lastMessage: '阿姨，明天上午九点我准时在楼下等您！',
    lastTime: '16 : 56',
    unreadCount: 0
  },
  {
    sessionId: 'sess_wang',
    targetUser: {
      id: 'c_wang',
      name: '王秋华',
      avatar: '/static/avatars/wangqiuhua.png',
      tag: '@村委会干事'
    },
    lastMessage: '张老师，社区端午节的慰问品明天统一发放。',
    lastTime: '20 : 58',
    unreadCount: 0
  }
];

export const initialChatMessages: Record<string, ChatMessage[]> = {
  sess_li: [
    {
      id: 'm1',
      sessionId: 'sess_li',
      senderId: 'c_li',
      receiverId: 'u_zhangdongmei',
      type: 'text',
      content: '杨老太，明天晚上做我的舞伴去跳广场舞啊。',
      createdAt: '19:50',
      timestamp: Date.now() - 600000
    },
    {
      id: 'm2',
      sessionId: 'sess_li',
      senderId: 'u_zhangdongmei',
      receiverId: 'c_li',
      type: 'text',
      content: '好啊，先这样约好',
      createdAt: '19:58',
      timestamp: Date.now() - 120000
    }
  ]
};

export const initialPosts: PostItem[] = [
  {
    id: 'post_001',
    authorId: 'u_wangnainai',
    authorName: '王奶奶',
    authorAvatar: '/static/avatars/wangnainai.png',
    content: '马玲玲那孩子脾气好又有耐心',
    likeCount: 12,
    commentCount: 1,
    timeAgo: '1分钟前',
    isLiked: false
  },
  {
    id: 'post_002',
    authorId: 'u_zhangdongmei',
    authorName: '张冬梅',
    authorAvatar: '/static/avatars/zhangdongmei.png',
    content: '感谢吴小应同学今天陪我去第一医院做检查，跑前跑后非常周到，现在的大学生真有爱心！',
    likeCount: 28,
    commentCount: 4,
    timeAgo: '昨天',
    isLiked: true
  }
];

export const initialGoods: MallGoods[] = [
  {
    id: 'g_egg',
    name: '鸡蛋/盒',
    price: 1000,
    image: '/static/goods/egg.jpg',
    unit: '盒',
    stock: 99
  },
  {
    id: 'g_bag',
    name: '袋子/捆',
    price: 50,
    image: '/static/goods/bag.jpg',
    unit: '捆',
    stock: 300
  },
  {
    id: 'g_paste',
    name: '牙膏/盒',
    price: 100,
    image: '/static/goods/toothpaste.jpg',
    unit: '盒',
    stock: 120
  },
  {
    id: 'g_tissue',
    name: '纸巾/盒',
    price: 500,
    image: '/static/goods/tissue.jpg',
    unit: '盒',
    stock: 180
  },
  {
    id: 'g_culture_fan',
    name: '文创团扇/把',
    price: 800,
    image: '/static/goods/fan.jpg',
    unit: '把',
    stock: 60
  },
  {
    id: 'g_culture_bag',
    name: '文创手提袋/个',
    price: 600,
    image: '/static/goods/paper-bag.jpg',
    unit: '个',
    stock: 80
  }
];
