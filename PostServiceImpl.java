package com.fd.service.community.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.fd.provide.community.PostService;
import com.fd.provide.community.pojo.CommentObj;
import com.fd.provide.community.pojo.PostObj;
import com.fd.provide.community.pojo.ReplyObj;
import com.fd.provide.community.pojo.base.SimpleUser;
import com.fd.service.community.convert.CommentObj2Comment;
import com.fd.service.community.convert.Post2PostObj;
import com.fd.service.community.convert.ReplyObj2Reply;
import com.fd.service.community.dao.PostMongodbDao;
import com.fd.service.community.dao.UserMongodbDao;
import com.fd.service.community.pojo.Comment;
import com.fd.service.community.pojo.Post;
import com.fd.service.community.pojo.Reply;
import com.fd.service.community.pojo.User;
import com.github.pagehelper.util.StringUtil;

@Service("postService")
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    @Autowired
    private PostMongodbDao postMongodbDao;

    @Autowired
    private UserMongodbDao userMongodbDao;

    @Override
    public void discuss(PostObj postObj) {
    }

    @Override
    public PostObj savePost(PostObj postObj) {
        Post post = Post2PostObj.convertToPost(postObj, null);
        if (StringUtil.isEmpty(post.getMsg())) {
            post.setMsg("");
        }
        if (CollectionUtils.isEmpty(post.getPictureurl())) {
            post.setPictureurl(new ArrayList<String>());
        }
        if (StringUtil.isEmpty(post.getVideourl())) {
            post.setVideourl("");
        }
        if (post.getPostTime() == 0L) {
            post.setPostTime(System.currentTimeMillis());
        }
        if (StringUtil.isEmpty(post.getPostPic())) {
            post.setPostPic("");
        }
        if (post.getAuthor() == null) {
            post.setAuthor(new SimpleUser());
        }
        if (StringUtil.isEmpty(post.getAuthor().getHeadurl())) {
            post.getAuthor().setHeadurl("");
        }
        if (StringUtil.isEmpty(post.getAuthor().getName())) {
            post.getAuthor().setName("");
        }

        if (CollectionUtils.isEmpty(post.getCommentList())) {
            post.setCommentList(new ArrayList<Comment>());
        }
        if (CollectionUtils.isEmpty(post.getPraiseList())) {
            post.setPraiseList(new ArrayList<SimpleUser>());
        }
        try {
            postMongodbDao.save(post);

            postObj = Post2PostObj.convertToPostObj(post, null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return postObj;
    }

    @Override
    public PostObj getPostDescByPostId(int loginUserId, int postId) {
        try {
            Post post = postMongodbDao.getPostDescByPostId(postId);
            PostObj postObj = Post2PostObj.convertToPostObj(post, null);
            if (postObj != null) {
                // 是否点赞
                if (postObj.getPraiseList() != null) {
                    for (SimpleUser user : postObj.getPraiseList()) {
                        if (user.getUserId() == loginUserId) {
                            postObj.setIsPraised(true);
                            break;
                        }
                    }
                }
                // 是否关注
                if (postObj.getAuthor() != null && postObj.getAuthor().getUserId() != 0) {
                    if(postObj.getAuthor().getUserId() == loginUserId){
                        postObj.getAuthor().setIsFollow(2);
                    }else {
                        User user = userMongodbDao.queryByUserId(loginUserId);
                        for (SimpleUser simpleUser : user.getFollow()) {
                            if (simpleUser.getUserId() == postObj.getAuthor().getUserId()) {
                                postObj.getAuthor().setIsFollow(1);
                                break;
                            }
                        }
                    }
                }
                // 评论分页
                List<CommentObj> commentObjs = getCommentList(postId, 10, 1);
                if (commentObjs != null) {
                    // 回复评论分页
                    for (CommentObj commentObj : commentObjs) {
                        List<ReplyObj> replyObjs = getReplyList(postId, commentObj.getCommentId(), 1, 10);
                        boolean hasReplyListNext = hasReplyListNext(postId, commentObj.getCommentId(), 2, 10);
                        commentObj.setHasReplyListNext(hasReplyListNext);
                        commentObj.setReplyList(replyObjs);
                    }
                }
                postObj.setCommentList(commentObjs);
                // 是否有下一页评论
                postObj.setHasCommentListNext(hasCommentListNext(postId, 10, 2));
            }
            return postObj;
        } catch (Exception e) {
            logger.error("获取帖子详情出错：" + e.getMessage());
        }
        return null;
    }

    @Override
    public CommentObj getCommentDescByPostIdAndCommentId(int postId, int commentId) {
        try {
            Post post = postMongodbDao.getPostDescByPostId(postId);
            PostObj postObj = Post2PostObj.convertToPostObj(post, null);
            CommentObj commentObj = null;
            if (postObj != null && postObj.getCommentList() != null) {
                postObj.setCommentList(JSONObject.parseArray(JSONObject.toJSONString(post.getCommentList()), CommentObj.class));
                for (CommentObj item : postObj.getCommentList()) {
                    if (item.getCommentId() == commentId) {
                        commentObj = item;
                        break;
                    }
                }
            }
            if (commentObj != null) {
                List<ReplyObj> replyObjs = getReplyList(postId, commentObj.getCommentId(), 1, 10);
                boolean hasReplyListNext = hasReplyListNext(postId, commentObj.getCommentId(), 2, 10);
                commentObj.setHasReplyListNext(hasReplyListNext);
                commentObj.setReplyList(replyObjs);
            }
            return commentObj;
        } catch (Exception e) {
            logger.error("获取评论详情出错：" + e.getMessage());
        }
        return null;
    }

    /**
     * 是否有下一页评论
     *
     * @param postId
     * @param pageSize
     * @param pageNum
     * @return
     */
    private boolean hasCommentListNext(int postId, int pageSize, int pageNum) {
        List<CommentObj> commentList = getCommentList(postId, pageSize, pageNum);
        return (commentList == null || commentList.isEmpty()) ? false : true;
    }

    /**
     * 是否有下一页回复
     *
     * @param postId
     * @param commentId
     * @param pageNum
     * @param pageSize
     * @return
     */
    private boolean hasReplyListNext(int postId, int commentId, int pageNum, int pageSize) {
        List<ReplyObj> replyObjs = getReplyList(postId, commentId, pageNum, pageSize);
        return (replyObjs == null || replyObjs.isEmpty()) ? false : true;
    }

    @Override
    public int deletePost(int id) {
        try {
            postMongodbDao.deleteById(id);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return 0;
        }
        return 1;
    }

    @Override
    public int commentPost(int postId, SimpleUser user, String msg) {
        try {
            if (user == null) {
                return 0;
            }
            if (StringUtil.isEmpty(user.getName())) {
                user.setName("");
            }
            if (StringUtil.isEmpty(user.getHeadurl())) {
                user.setHeadurl("");
            }
            CommentObj commentObj = new CommentObj();
            commentObj.setFromUser(user);
            commentObj.setMsg(msg);
            commentObj.setCommentTime(System.currentTimeMillis());
            commentObj.setReplyCount(0);
            commentObj.setState(0);
            commentObj.setReplyList(new ArrayList<ReplyObj>());

            Comment comment = CommentObj2Comment.convertToComment(commentObj, null);

            postMongodbDao.commentPost(postId, comment);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return 0;
        }
        return 1;
    }

    @Override
	public List<PostObj> getPostList(Integer userId, int loginUserId, int pageNum, int pageSize) {
		if (pageNum < 1) {
			pageNum = 1;
		}
		if (pageSize < 1) {
			pageSize = 10;
		}

        int startNum = (pageNum - 1) * pageSize;
        int limitNum = pageSize;
        Query query = new Query();
        //1审核通过
        query.addCriteria(Criteria.where("state").is(1));
        if (userId != null) {
            query.addCriteria(Criteria.where("author.userId").is(userId));
        }
        query.with(Sort.by(new Order(Direction.DESC, "postId")));
        query.skip(startNum);// 起点
        query.limit(limitNum);// 长度
        List<Post> dataList = postMongodbDao.getPage(query);
        List<PostObj> returnList = Post2PostObj.convertCollectionToPostObj(dataList);
		if (returnList == null) {
			return new ArrayList<PostObj>();
		}
		for (PostObj postObj : returnList) {
			if (postObj.getPraiseList() != null) {
				for (SimpleUser item : postObj.getPraiseList()) {
					if (item.getUserId() == loginUserId) {
						postObj.setIsPraised(true);
						break;
					}
				}
			}
		}
		return returnList;
	}

    @Override
    public int replyComment(ReplyObj replyObj) {
        try {
            SimpleUser fromUserObj = replyObj.getFromUser();
            if (fromUserObj == null) {
                fromUserObj = new SimpleUser();
                fromUserObj.setHeadurl("");
                fromUserObj.setName("");
                replyObj.setFromUser(fromUserObj);
            }
            SimpleUser toUserObj = replyObj.getToUser();
            if (toUserObj == null) {
                toUserObj = new SimpleUser();
                toUserObj.setHeadurl("");
                toUserObj.setName("");
                replyObj.setToUser(toUserObj);
            }

            Reply reply = ReplyObj2Reply.convertToReply(replyObj, null);

            postMongodbDao.replyComment(reply);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return 0;
        }
        return 1;
    }

    @Override
    public List<CommentObj> getCommentList(int postId, int pageSize, int pageNum) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        List<Comment> list = postMongodbDao.getCommentList(postId, pageSize, pageNum);
        List<CommentObj> returnList = CommentObj2Comment.convertCollectionToCommentObj(list);
        if (returnList != null) {
            for (CommentObj commentObj : returnList) {
                List<ReplyObj> replyList = commentObj.getReplyList();
                int limitNum = pageSize;
                int size = 0;
                if (replyList != null) {
                    size = replyList.size();
                    if (size < limitNum) {
                        limitNum = size;
                        commentObj.setHasReplyListNext(false);
                    } else {
                        commentObj.setHasReplyListNext(true);
                    }
                    commentObj.setReplyList(replyList.subList(0, limitNum));
                }
            }
        }
        return returnList;
    }

    @Override
    public void praise(Integer fromUserId, Integer postId, int opt) {
        postMongodbDao.praise(fromUserId, postId, opt);
    }

    @Override
    public List<PostObj> usercenterPost(Integer userId, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        List<Post> list = postMongodbDao.usercenterPost(userId, pageSize, pageNum);
        return Post2PostObj.convertCollectionToPostObj(list);
    }

    @Override
    public boolean replyComment(SimpleUser simpleUser, int postId, int commentId, int toUserId, String msg) {

        return postMongodbDao.replyComment(simpleUser, postId, commentId, toUserId, msg);
    }

    @Override
    public List<ReplyObj> getReplyList(int postId, int commentId, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        List<Reply> list = postMongodbDao.getReplyList(postId, commentId, pageNum, pageSize);
        return ReplyObj2Reply.convertReplyToReplyObj(list);
    }

    @Override
    public List<SimpleUser> getPraiseList(int loginUserId, int postId, int pageNum, int pageSize) {
        try {
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1) {
                pageSize = 10;
            }
            List<SimpleUser> list = postMongodbDao.getPraiseList(postId, pageNum, pageSize);
            if (list != null) {
                for (SimpleUser simpleUser : list) {
                    if (simpleUser.getUserId() == loginUserId) {
                        simpleUser.setIsFollow(2);
                        continue;
                    }
                    User user = userMongodbDao.queryByUserId(simpleUser.getUserId());
                    //当前用户，是否关注了（点赞列表）用户，0否，1是
                    int isFollow = 0;
                    if (user != null) {
                        List<SimpleUser> followList = user.getFollow();
                        for (SimpleUser item : followList) {
                            if (item.getUserId() == loginUserId) {
                                isFollow = 1;
                                break;
                            }
                        }
                    }
                    simpleUser.setIsFollow(isFollow);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("获取点赞列表出错：" + e.getMessage());
        }
        return null;
    }
}
