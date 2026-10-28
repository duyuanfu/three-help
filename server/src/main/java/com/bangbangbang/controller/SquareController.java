package com.bangbangbang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.Post;
import com.bangbangbang.entity.PostComment;
import com.bangbangbang.mapper.PostCommentMapper;
import com.bangbangbang.mapper.PostMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    public SquareController(PostMapper postMapper, PostCommentMapper commentMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
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

    @GetMapping
    public ApiResult<List<Post>> list() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_no");
        List<Post> list = postMapper.selectList(wrapper);
        return ApiResult.ok(list == null ? new ArrayList<>() : list);
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
        return ApiResult.ok(list == null ? new ArrayList<>() : list);
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
