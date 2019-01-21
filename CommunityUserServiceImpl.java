package com.fd.service.community.impl;

import com.fd.provide.community.CommunityUserService;
import com.fd.provide.community.pojo.MsgObj;
import com.fd.provide.community.pojo.UserObj;
import com.fd.provide.community.pojo.base.SimpleUser;
import com.fd.service.community.convert.Msg2MsgObj;
import com.fd.service.community.convert.User2UserObj;
import com.fd.service.community.dao.UserMongodbDao;
import com.fd.service.community.pojo.Msg;
import com.fd.service.community.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("communityUserService")
public class CommunityUserServiceImpl implements CommunityUserService {

    private static final Logger logger = LoggerFactory.getLogger(CommunityUserServiceImpl.class);

    @Autowired
    private UserMongodbDao mongodbDao;
    @Autowired
    private UserMongodbDao test2;
	
	@Autowired
    private UserMongodbDao mongodbDao;
	
	 @Autowired
    private UserMongodbDao c;
	
	 @Autowired
    private UserMongodbDao b;
    
    @Override
    public UserObj queryByUserId(int userId) {
    	User user = mongodbDao.queryByUserId(userId);
        UserObj userObj = User2UserObj.convertToUserObj(user, null);
        return userObj;
    }

    @Override
    public void follow(Integer fans, Integer follower, int opt) {
        mongodbDao.follow(fans, follower, opt);
    }

    @Override
    public List<SimpleUser> usercenterFollow(Integer userId, int pageNum, int pageSize, String name) {
	return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
	return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);

return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
        return mongodbDao.usercenterFollow(userId, pageNum, pageSize, name);
    }

    @Override
    public List<MsgObj> getUserMsgList(int userId, int pageSize, int pageNum) {
        if (pageNum < 1) {
            pageNum = 1;
		}
    	if (pageSize < 1) {
    		pageSize = 10;
		}
        List<Msg> list = mongodbDao.getUserMsgList(userId, pageSize, pageNum);
		return Msg2MsgObj.convertCollectionToMsgObj(list);
    }


    @Override
    public void saveOrUpdateUser(UserObj userObj) {
        if (queryByUserId(userObj.getUserId()) == null) {
            User user = User2UserObj.convertToUser(userObj, null);
            mongodbDao.save(user);
        } else {
            User user = User2UserObj.convertToUser(userObj, null);
            this.mongodbDao.saveOrUpdateUser(user);
        }
    }
	
	    @Override
    public void saveOrUpdateUser(UserObj userObj) {
        if (queryByUserId(userObj.getUserId()) == null) {
            User user = User2UserObj.convertToUser(userObj, null);
            mongodbDao.save(user);
        } else {
            User user = User2UserObj.convertToUser(userObj, null);
            this.mongodbDao.saveOrUpdateUser(user);
        }
    }
}
