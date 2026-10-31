package com.bangbangbang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.Post;
import com.bangbangbang.entity.PostComment;
import com.bangbangbang.mapper.PostCommentMapper;
import com.bangbangbang.mapper.PostMapper;
import com.bangbangbang.service.UserDisplayService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 广场：发动态、点赞、评论、删除
 *
 * 作者信息一律取自登录令牌，前端传入的 authorId 等字段会被忽略。
 */
@RestController
@RequestMapping("/api/posts")
public class SquareController {

    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final UserDisplayService userDisplayService;

    public SquareController(PostMapper postMapper, PostCommentMapper commentMapper,
                            UserDisplayService userDisplayService) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.userDisplayService = userDisplayService;
    }

    @Data
    public static class CreatePostRequest {
        private String content;
        private List<String> images;
    }

    @Data
    public static class CreateCommentRequest {
        private String content;
    }

    /** 分页结果：nextCursor 为 null 表示已经到底 */
    @Data
    public static class PostPage {
        private List<Post> items;
        private Long nextCursor;
        private Boolean hasMore;
    }

    /**
     * 广场动态分页（游标 = sort_no，不用 offset）。
     *
     * sort_no 越小越新（发帖时取「当前最小值 - 1」），所以按它升序遍历就是「新 → 旧」。
     * 下一页条件是 sort_no > cursor：翻页期间别人发了新帖也不会出现重复或漏项，
     * 而 offset 分页会因为前面插入了新行整体错位。
     */
    @GetMapping("/feed")
    public ApiResult<PostPage> feed(@RequestParam(required = false) Long cursor,
                                    @RequestParam(defaultValue = "20") Integer limit,
                                    @RequestParam(required = false) String keyword) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        if (cursor != null) {
            wrapper.gt("sort_no", cursor);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 搜索在服务端做：只加载了前几页时，前端本地过滤是搜不全的
            wrapper.like("author_name", keyword.trim());
        }
        wrapper.orderByAsc("sort_no");
        return ApiResult.ok(page(wrapper, limit));
    }

    /** 我发布的动态（「我的-我的动态」用，不受广场分页影响） */
    @GetMapping("/mine")
    public ApiResult<PostPage> mine(@RequestParam(required = false) Long cursor,
                                    @RequestParam(defaultValue = "20") Integer limit) {
        String meId = UserContext.require().id();
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", meId);
        if (cursor != null) {
            wrapper.gt("sort_no", cursor);
        }
        wrapper.orderByAsc("sort_no");
        return ApiResult.ok(page(wrapper, limit));
    }

    /** 单条动态：评论页直接按 id 打开时用，不依赖列表是否已经加载过这一条 */
    @GetMapping("/{id}")
    public ApiResult<Post> detail(@PathVariable String id) {
        UserContext.require();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw BizException.of("动态不存在");
        }
        userDisplayService.fillPosts(Collections.singletonList(post));
        return ApiResult.ok(post);
    }

    /** 多取一条判断有没有下一页，避免额外的 count 查询 */
    private PostPage page(QueryWrapper<Post> wrapper, Integer limit) {
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        wrapper.last("LIMIT " + (size + 1));
        List<Post> list = postMapper.selectList(wrapper);
        if (list == null) {
            list = new ArrayList<>();
        }
        boolean hasMore = list.size() > size;
        if (hasMore) {
            list = new ArrayList<>(list.subList(0, size));
        }
        userDisplayService.fillPosts(list);

        PostPage result = new PostPage();
        result.setItems(list);
        // sort_no 为空说明数据异常，直接按「没有更多」处理，避免前端反复请求同一页
        Long last = list.isEmpty() ? null : list.get(list.size() - 1).getSortNo();
        boolean canContinue = hasMore && last != null;
        result.setHasMore(canContinue);
        result.setNextCursor(canContinue ? last : null);
        return result;
    }

    @PostMapping
    public ApiResult<Post> create(@RequestBody CreatePostRequest req) {
        UserContext.CurrentUser me = UserContext.require();
        String content = req.getContent() == null ? "" : req.getContent().trim();
        List<String> images = req.getImages() == null ? new ArrayList<>() : req.getImages();
        if (content.isEmpty() && images.isEmpty()) {
            throw BizException.of("说点什么或选张图片吧");
        }

        Post post = new Post();
        post.setId("post_" + System.currentTimeMillis());
        post.setAuthorId(me.id());
        post.setAuthorName(me.name());
        post.setAuthorAvatar(me.avatar());
        post.setAuthorTag(me.tag());
        post.setContent(content);
        post.setImages(images);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setTimeAgo("刚刚");
        post.setIsLiked(false);
        post.setSortNo(nextPostSortNo());
        postMapper.insert(post);
        return ApiResult.ok(post);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable String id) {
        String meId = UserContext.require().id();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw BizException.of("动态不存在");
        }
        if (!meId.equals(post.getAuthorId())) {
            throw BizException.of("只能删除自己发布的动态");
        }
        postMapper.deleteById(id);

        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", id);
        commentMapper.delete(wrapper);
        return ApiResult.ok();
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/{id}/like")
    public ApiResult<Post> toggleLike(@PathVariable String id) {
        UserContext.require();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw BizException.of("动态不存在");
        }
        boolean liked = Boolean.TRUE.equals(post.getIsLiked());
        int count = post.getLikeCount() == null ? 0 : post.getLikeCount();
        post.setIsLiked(!liked);
        post.setLikeCount(Math.max(0, liked ? count - 1 : count + 1));
        postMapper.updateById(post);
        return ApiResult.ok(post);
    }

    @GetMapping("/{id}/comments")
    public ApiResult<List<PostComment>> comments(@PathVariable String id) {
        UserContext.require();
        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", id).orderByAsc("sort_no");
        List<PostComment> list = commentMapper.selectList(wrapper);
        if (list == null) {
            list = new ArrayList<>();
        }
        userDisplayService.fillComments(list);
        return ApiResult.ok(list);
    }

    @PostMapping("/{id}/comments")
    public ApiResult<PostComment> addComment(@PathVariable String id, @RequestBody CreateCommentRequest req) {
        UserContext.CurrentUser me = UserContext.require();
        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isEmpty()) {
            throw BizException.of("评论内容不能为空");
        }
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw BizException.of("动态不存在");
        }

        PostComment comment = new PostComment();
        comment.setId("cmt_" + System.nanoTime());
        comment.setPostId(id);
        comment.setAuthorId(me.id());
        comment.setUserName(me.name());
        comment.setAvatar(me.avatar());
        comment.setContent(content);
        comment.setSortNo(nextCommentSortNo(id));
        commentMapper.insert(comment);

        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", id);
        Long total = commentMapper.selectCount(wrapper);
        post.setCommentCount(total == null ? 0 : total.intValue());
        postMapper.updateById(post);

        return ApiResult.ok(comment);
    }

    /* ------------------------------------------------------------------ */

    private long nextPostSortNo() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select("MIN(sort_no) AS agg");
        List<java.util.Map<String, Object>> maps = postMapper.selectMaps(wrapper);
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                return number.longValue() - 1;
            }
        }
        return 0L;
    }

    private long nextCommentSortNo(String postId) {
        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId).select("MAX(sort_no) AS agg");
        List<java.util.Map<String, Object>> maps = commentMapper.selectMaps(wrapper);
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                return number.longValue() + 1;
            }
        }
        return 1L;
    }
}
