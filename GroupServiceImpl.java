package com.fd.service.community.impl;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fd.provide.community.GroupService;
import com.fd.provide.community.pojo.GroupObj;
import com.fd.service.community.convert.Group2GroupObj;
import com.fd.service.community.mapper.GroupMapper;
import com.fd.service.community.pojo.Group;
import com.fd.service.community.util.HttpClientUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Map.Entry;

@Service("groupService")
public class GroupServiceImpl implements GroupService {

	private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

	/**
	 * 創建群url
	 */
	private static final String GROUPURL = "https://console.tim.qq.com/v4/group_open_http_svc/create_group";
	/**
	 * 批量添加群成員url
	 */
	private static final String ADDGROUPMEMBERURL = "https://console.tim.qq.com/v4/group_open_http_svc/add_group_member";
	/**
	 * 根據Identifier設置資料url
	 */
    private static final String PORTRAITSETURL = "https://console.tim.qq.com/v4/profile/portrait_set";
    /**
     * 删除群组
     */
    private static final String DELETEGROUPMEMBER = "https://console.tim.qq.com/v4/group_open_http_svc/delete_group_member";

	/**
	 * 获取用户所加入的群组
	 */
	private static final String GETJOINEDGROUPLIST="https://console.tim.qq.com/v4/group_open_http_svc/get_joined_group_list";

	/**
	 * 修改群基础资料
	 */
	private static final String MODIFYGROUPBASEINFO = "https://console.tim.qq.com/v4/group_open_http_svc/modify_group_base_info";

	/**
	 * 注册
	 * usersig=xxx&identifier=admin&sdkappid=88888888&random=99999999&contenttype=json
	 */
	private static final String REGISTER = "https://console.tim.qq.com/v4/im_open_login_svc/multiaccount_import";
	
	/**
	 * 修改群成员资料
	 * usersig=xxx&identifier=admin&sdkappid=88888888&random=99999999&contenttype=json
	 */
	private static final String UPDATEGROUPMEMBERINFO = "https://console.tim.qq.com/v4/group_open_http_svc/modify_group_member_info";
	/**
	 * 获取群组详细资料
	 * usersig=xxx&identifier=admin&sdkappid=88888888&random=99999999&contenttype=json
	 */
	private static final String GETGROUPINFO = "https://console.tim.qq.com/v4/group_open_http_svc/get_group_info";
	
	@Autowired
	private GroupMapper groupMapper;
	
    @Value("${im.sdkAppId}")
    private String sdkAppId;
    
    @Value("${im.admin}")
    private String admin;

    @Value("${im.limit}")
    private String limit;
    
    @Value("${im.groupName4}")
    private String groupName4;
    
    @Value("${im.groupName1}")
    private String groupName1;
    
    @Value("${im.groupName2}")
    private String groupName2;
    
    @Value("${im.groupName3}")
    private String groupName3;
    
    @Value("${im.groupName7}")
    private String groupName7;
    
    @Value("${im.groupPic4}")
    private String groupPic4;
    
    @Value("${im.groupPic2}")
    private String groupPic2;
    
    @Value("${im.groupPic3}")
    private String groupPic3;
    
    @Value("${im.groupPic7}")
    private String groupPic7;

	@Override
	@Transactional
	public Map<String, String> creatGroup(String usersig, String identifier, String Owner_Account,
			String Type, String Name, int level) throws RuntimeException{
		
		HashMap<String, String> results = new HashMap<String, String>();
		// 先增加到数据库
		Group group = new Group();
		// 新加的，只有群主一个
		group.setGroupCount(1);
		// 先设置空，等调用完成腾讯云创建群成功之后再设置groupId
		group.setGroupId("");
		group.setGroupLevel(level);
		group.setGroupLimit(Integer.valueOf(limit));
		group.setGroupName(Name);
		group.setGroupPic(getPicByLevel(level));
		group.setGroupOwner(Owner_Account);
		if (groupMapper.insertSelective(group) <= 0) {
			logger.info("创建群失败");
			results.put("code", "FAIL");
			results.put("ErrorInfo", "数据库创建群失败");
			return results;
		}
		
		HashMap<String, String> params = new HashMap<String, String>();
		HashMap<String, String> payload = new HashMap<String, String>();

		payload.put("Owner_Account", Owner_Account);
		payload.put("Type", Type);
		payload.put("Name", Name);
		JSONObject payloadjson = JSONObject.parseObject(JSON.toJSONString(payload));

		params.put("usersig", usersig);
		params.put("identifier", identifier);
		params.put("sdkappid", sdkAppId);
		params.put("contenttype", "json");

		/**
		 * ActionStatus String 请求处理的结果，OK 表示处理成功，FAIL 表示失败。 ErrorCode Integer
		 * 错误码。 ErrorInfo String 错误信息。 GroupId String 创建成功之后的群 ID，由 IM 云后台分配。
		 **/
		JSONObject result = HttpClientUtils.Post(GROUPURL, payloadjson, params);
		if ("OK".equals(result.get("ActionStatus"))) {
			logger.info("创建群成功");
			results.put("code", "OK");
			results.put("GroupId", result.get("GroupId").toString());
			
			// 修改groupId
			group.setGroupId(result.get("GroupId").toString());
			if (groupMapper.updateByPrimaryKeySelective(group) <= 0) {
				logger.error("修改群groupId失败");
			}
			
			// 修改群头像
			Map<String, String> fileds = new HashMap<String, String>();
			fileds.put("FaceUrl", getPicByLevel(level));
			modifyGroupBaseinfo(usersig, identifier, result.get("GroupId").toString(), fileds);
		} else {
			throw new RuntimeException(result.get("ErrorInfo").toString());
		}

		return results;
	}

	private String getPicByLevel(int level) {
		String group_pic = "";
		switch (level) {
		case 2:
			group_pic = groupPic2;
			break;
		case 3:
			group_pic = groupPic3;
			break;
		case 4:
			group_pic = groupPic4;
			break;
		case 7:
			group_pic = groupPic7;
			break;

		default:
			break;
		}
		return group_pic;
	}

	@Override
	@Transactional
	public Map<String, Object> addGroupMember(String accountSig, String accountIdentifier, String groupId,
			   Integer silence, List<String> memberList)  throws RuntimeException{
		HashMap<String, Object> results = new HashMap<String, Object>();
		if (memberList.size() > 500) {
			logger.error("批量添加群成員失敗");
			results.put("code", "FAIL");
			results.put("ErrorInfo", "每次最多批量添加500個群成員");
			return results;
		}
		if (CollectionUtils.isEmpty(memberList)) {
			logger.error("批量添加群成員失敗");
			results.put("code", "FAIL");
			results.put("ErrorInfo", "要添加群成員列表為空");
			return results;
		}
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("usersig", accountSig);
		params.put("identifier", accountIdentifier);
		params.put("sdkappid", sdkAppId);
		params.put("contenttype", "json");

		JSONObject[] members = new JSONObject[memberList.size()];
		int i = 0;
		JSONObject jsonObject;
		for (String string : memberList) {
			jsonObject = new JSONObject();
			jsonObject.put("Member_Account", string);
			members[i++] = jsonObject;
		}
		JSONObject payloadjson = new JSONObject();
		payloadjson.put("MemberList", members);
		payloadjson.put("GroupId", groupId);
		payloadjson.put("Silence", silence);
		
		// 修改群的人数
		Group group = groupMapper.selectByGroupId(groupId);
		if (group == null) {
			logger.error("群不存在");
			results.put("code", "FAIL");
			results.put("ErrorInfo", "群不存在");
			return results;
		}
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("id", group.getId());
		map.put("count", memberList.size());
		if (groupMapper.addGroupCount(map) <= 0) {
			logger.error("增加群人数失败");
			results.put("code", "FAIL");
			results.put("ErrorInfo", "增加群人数失败");
			return results;
		}

		// ActionStatus String 请求处理的结果，OK 表示处理成功，FAIL 表示失败。
		// ErrorCode Integer 错误码。
		// ErrorInfo String 错误信息。
		// MemberList Array 返回添加的群成员结果。
		// 1.Member_Account String 返回的群成员帐号。
		// 2.Result Integer 加人结果：0-失败；1-成功；2-已经是群成员；3-等待被邀请者确认。
		// 错误码说明 ：除非发生网络错误（例如 502 错误），该接口的 HTTP 返回码均为
		// 200。真正的错误码、错误信息是通过应答包体中的ErrorCode、ErrorInfo 来表示的。
		// 公共错误码（60000 到 79999）
		JSONObject result = HttpClientUtils.Post(ADDGROUPMEMBERURL, payloadjson, params);
		
		if ("OK".equals(result.get("ActionStatus"))) {
			logger.info("批量添加群成員成功");
			results.put("code", "OK");
			results.put("MemberList", result.get("MemberList"));
		} else {
			logger.error("批量添加群成員失敗");
			throw new RuntimeException(result.get("ErrorInfo").toString());
		}
		return results;
	}
	
	@Override
	public boolean register(String accountSig, String accountIdentifier, String account) {
		logger.info("注册成员：");
		String[] accounts = {account};
        HashMap<String, Object> jsonmap = new HashMap<>();
        jsonmap.put("Accounts", accounts);
        JSONObject json = JSONObject.parseObject(JSON.toJSONString(jsonmap));
        JSONObject result = HttpClientUtils.Post(REGISTER, json, getGroupParam(accountIdentifier, accountSig));
        /**
{
    "ActionStatus": "OK",
    "ErrorInfo": "",
    "ErrorCode": 0
}
ActionStatus	String	请求处理的结果，OK表示处理成功，FAIL表示失败。
ErrorCode	Integer	错误码。
ErrorInfo	String	错误信息。
         */
		return result.containsKey("ActionStatus") && "OK".equals(result.getString("ActionStatus"));

	}

	@Override
	public boolean portraitSet(String usersig, String adminIdentifier, String targetIdentifier,
			HashMap<String, String> tags) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("usersig", usersig);
		params.put("identifier", adminIdentifier);
		params.put("sdkappid", sdkAppId);
		params.put("contenttype", "json");

		JSONObject[] ProfileItem = new JSONObject[tags.size()];
		int i = 0;
		JSONObject jsonObject;
		for (Entry<String, String> tag : tags.entrySet()) {
			jsonObject = new JSONObject();
			jsonObject.put("Tag", tag.getKey());
			jsonObject.put("Value", tag.getValue());
			ProfileItem[i++] = jsonObject;
		}

		JSONObject payloadjson = new JSONObject();
		payloadjson.put("From_Account", targetIdentifier);
		payloadjson.put("ProfileItem", ProfileItem);

		// ActionStatus String 请求处理的结果，“OK” 表示处理成功，“FAIL” 表示失败。
		// ErrorCode Integer 错误码。
		// ErrorInfo String 详细错误信息。
		// ErrorDisplay String 详细的客户端展示信息。
		JSONObject result = HttpClientUtils.Post(PORTRAITSETURL, payloadjson, params);
		if ("OK".equals(result.get("ActionStatus"))) {
			return true;
		}
		logger.error("修改资料失敗" + result.get("ErrorInfo").toString());
		return false;
	}

    /**
     * https://console.tim.qq.com/v4/group_open_http_svc/delete_group_member
     * ?usersig=xxx
     * &identifier=admin
     * &sdkappid=88888888
     * &random=99999999
     * &contenttype=json
     */
    @Override
    @Transactional
	public boolean deleteGroupMember(String groupSig, String groupIdentifier, String groupId, String[] accounts) throws RuntimeException{
		logger.info("删除群成员：");
		logger.info("GroupId = [" + groupId + "], accounts = [" + Arrays.toString(accounts) + "]");
		
		if (accounts == null || accounts.length == 0) {
			return false;
		}
		
		// 修改群的人数
		Group group = groupMapper.selectByGroupId(groupId);
		if (group == null) {
			logger.error("群不存在");
			return false;
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("id", group.getId());
		map.put("count", accounts.length);
		if (groupMapper.reduceGroupCount(map) <= 0) {
			logger.error("减少群人数失败");
			return false;
		}
		
        HashMap<String, Object> jsonmap = new HashMap<>();
        jsonmap.put("GroupId", groupId);
        jsonmap.put("Silence", 1);
        jsonmap.put("MemberToDel_Account", accounts);
        JSONObject json = JSONObject.parseObject(JSON.toJSONString(jsonmap));
        JSONObject result = HttpClientUtils.Post(DELETEGROUPMEMBER, json, getGroupParam(groupIdentifier, groupSig));
        /**
{
    "ActionStatus": "OK",
    "ErrorInfo": "",
    "ErrorCode": 0
}
ActionStatus	String	请求处理的结果，OK表示处理成功，FAIL表示失败。
ErrorCode	Integer	错误码。
ErrorInfo	String	错误信息。
         */
        if (result.containsKey("ActionStatus") && "OK".equals(result.getString("ActionStatus"))) {
        	return true;
		}
        
        throw new RuntimeException("删除群成员失败：" + result.getString("ErrorInfo"));
	}

	public HashMap<String, String> getGroupParam(String groupIdentifier, String groupSig){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("usersig", groupSig);
		params.put("identifier", groupIdentifier);
		params.put("sdkappid", sdkAppId);
		params.put("contenttype", "json");
		String rondom = RandomUtils.nextLong() + "";
		params.put("random", rondom);
		return params;
	}

	//	https://console.tim.qq.com/v4/group_open_http_svc/get_joined_group_list?
	// usersig=xxx
	// &identifier=admin
	// &sdkappid=88888888
	// &random=99999999
	// &contenttype=json

	/**
	 * 获取用户所加入的群组
	 */
	@Override
	public List<GroupObj> getJoinedGroupList(String userId, String accountIdentifier, String accountSig) {
		List<Group> groupList = new ArrayList<Group>();
		HashMap<String, Object> jsonmap = new HashMap<>();
		jsonmap.put("Member_Account", userId);
		JSONObject json = JSONObject.parseObject(JSON.toJSONString(jsonmap));
		JSONObject result = HttpClientUtils.Post(GETJOINEDGROUPLIST, json, getGroupParam(accountIdentifier, accountSig));
		/**
{
"ActionStatus": "OK",
"ErrorInfo": "",
"ErrorCode": 0,
"TotalCount": 2, // 不论Limit和Offset如何设置，该值总是满足条件的群组总数
"GroupIdList": [
    {
        "GroupId": "@TGS#2J4SZEAEL"
    },
    {
        "GroupId": "@TGS#2C5SZEAEF"
    }
]
}
		 */
		if (result.containsKey("ActionStatus") && "OK".equals(result.getString("ActionStatus"))) {
			JSONArray array = result.getJSONArray("GroupIdList");
			if (array != null && array.size() > 0) {
				Group group = null;
				for (int i = 0; i < array.size(); i++) {
					JSONObject item = array.getJSONObject(i);
					group = groupMapper.selectByGroupId(item.getString("GroupId"));
					if (group != null) {
						groupList.add(group);
					}
				}
			}
		}
		return Group2GroupObj.convertCollectionToGroupObj(groupList);
	}

	@Override
	public String getGroupOwner() {
		return admin;
	}
	
	/**
	 * 增加一个群成員
	 * @param groupsig
	 * @param groupidentifier
	 * @param groupId
	 * @param identifier
	 * @return
	 */
	@Override
	@Transactional
	public boolean addOneGroupMember(String groupSig, String groupIdentifier, String groupId,String identifier){
		List<String> MemberList = new ArrayList<String>();
    	MemberList.add(identifier);
    	try {
    		Map<String, Object> resultMap = addGroupMember(groupSig, groupIdentifier, groupId, 1, MemberList);
			return "OK".equals(resultMap.get("code"));
		} catch (RuntimeException e) {
			logger.error(e.getMessage());
			return false;
		}
	}
	
	@Override
	public String getGroupNameByLevel(int level) {
		String groupName = "无名群";
		switch (level) {
		case 4:
			groupName = groupName4;
			break;
		case 1:
			groupName = groupName1;
			break;
		case 2:
			groupName = groupName2;
			break;
		case 3:
			groupName = groupName3;
			break;
		case 7:
			groupName = groupName7;
			break;

		default:
			break;
		}
		return groupName;
	}
	
	@Override
	public String getNotFullGroupIdByLevel(int level) {
		Group group = groupMapper.getNotFullGroupIdByLevel(level);
		return group == null ? null:group.getGroupId();
	}
	
	@Override
	public int getFullGroupIdCountByLevel(int level) {
		
		return groupMapper.getFullGroupIdCountByLevel(level);
	}
	
	@Override
	@Transactional
	public void dealUserGroup(String identifier, int newLevel, String groupIdentifier, String groupSig,String nameCard,String headPic) throws RuntimeException {
		String groupId = null;
		// 暂时没有钻石群
		if (newLevel != 1) {
			// 找出newLevel中一个没满人的群，如果都满了，就增加一个newLevel群
			groupId = getNotFullGroupIdByLevel(newLevel);
			if (groupId == null) {
				// 满群的有多少
				int count = getFullGroupIdCountByLevel(newLevel) + 1;
				// 创建新群
				String groupName = getGroupNameByLevel(newLevel) + String.valueOf(count) + "群";
				groupId = createNewGroup(newLevel, groupName, groupIdentifier, groupSig);
			}
			
			// 先加入newLevel的群
			if (!addOneGroupMember(groupSig, groupIdentifier, groupId, identifier)) {
				throw new RuntimeException("加入newLevel的群，userId=" + identifier);
			}
			// 修改群成员信息
			updateGroupMemberInfo(groupSig, groupIdentifier, groupId, identifier, nameCard, headPic);
		}
		
		// 找出用户所在的群
		List<GroupObj> groups = getJoinedGroupList(identifier, groupIdentifier, groupSig);
		if (groups != null) {
			// 循环所有用户所在的群，一一剔除
			String[] accounts = {identifier};
			String oldGroupId = null;
			for (GroupObj group : groups) {
				//删除群成员
				oldGroupId = group.getGroupId();
				if (!oldGroupId.equals(groupId)) {
					deleteGroupMember(groupSig, groupIdentifier, oldGroupId, accounts);
				}
			}
		}
	}

	/**
	 * 创建新群
	 * @param level
	 * @return
	 */
	private String createNewGroup(int level,String groupName, String groupIdentifier, String groupSig) throws RuntimeException{
		String groupId = null;
		// 创建新群
		String type = "Public";
		try {
    		Map<String, String> resultMap = creatGroup(groupSig, groupIdentifier, groupIdentifier, type, groupName, level);
        	// 群id，增加群成员和修改群成员资料时用到
        	if ("OK".equals(resultMap.get("code"))) {
        		groupId = resultMap.get("GroupId");
    		}else {
    			logger.error("创建级别" + level + "群失败：" + resultMap.get("ErrorInfo"));
    			throw new RuntimeException("创建级别" + level + "群失败：" + resultMap.get("ErrorInfo"));
    		}
		} catch (Exception e) {
			logger.error("创建级别" + level + "群失败：" + e.getMessage());
			throw new RuntimeException("创建级别" + level + "群失败：" + e.getMessage());
		}
		return groupId;
	}

	@Override
	public Map<String, Object> modifyGroupBaseinfo(String usersig, String adminIdentifier, String groupId,
			Map<String, String> fileds) {
		logger.info("修改群基础资料开始");
		Map<String, Object> results = new HashMap<String, Object>();
		JSONObject payloadjson = new JSONObject();
		payloadjson.put("GroupId", groupId);
		if (fileds.get("Name") != null) {
			// 群名称
			payloadjson.put("Name", fileds.get("Name"));
		}
		if (fileds.get("Introduction") != null) {
			// 群简介
			payloadjson.put("Introduction", fileds.get("Introduction"));
		}
		if (fileds.get("Notification") != null) {
			// 群公告
			payloadjson.put("Notification", fileds.get("Notification"));
		}
		if (fileds.get("FaceUrl") != null) {
			// 群头像
			payloadjson.put("FaceUrl", fileds.get("FaceUrl"));
		}

		JSONObject result = HttpClientUtils.Post(MODIFYGROUPBASEINFO, payloadjson,
				getGroupParam(adminIdentifier, usersig));
		
		if ("OK".equals(result.get("ActionStatus"))) {
			logger.info("修改群基础资料成功");
			results.put("code", "OK");
			results.put("message", "修改群基础资料成功");
		} else {
			logger.info("修改群基础资料失敗");
			results.put("code", "FAIL");
			results.put("ErrorInfo", result.get("ErrorInfo"));
		}

		return results;
	}
	/**
	 * 修改群成员资料
	 * 
param = {
"GroupId": "@TGS#2CLUZEAEJ",  //要操作的群组（必填）
"Member_Account": "bob",  // 要操作的群成员（必填）
"NameCard":"asads",
"AppMemberDefinedData": [ //要操作的成员自定义字段（选填）
    {
        "Key":"headPic",  //要操作的群成员自定义字段Key
        "Value":""  //要设置的数据内容
    }
]
}
*/
	@Override
	public boolean updateGroupMemberInfo(String accountSig, String accountIdentifier,String groupId,String account,String nameCard,String headPic) {
		logger.info("修改群成员资料：");
		JSONObject param = new JSONObject();
		param.put("GroupId", groupId);
		param.put("Member_Account", account);
		param.put("NameCard", nameCard);
		String[] headPics = getHeadPic(headPic);
		JSONArray array = new JSONArray();
		array.add(getItem("member_head_pic",headPics[0]));
		array.add(getItem("member_head_pic1",headPics[1]));
		array.add(getItem("member_head_pic2",headPics[2]));
		param.put("AppMemberDefinedData", array);
		JSONObject result = HttpClientUtils.Post(UPDATEGROUPMEMBERINFO, param, getGroupParam(accountIdentifier, accountSig));
		if ("OK".equals(result.get("ActionStatus"))) {
			logger.info("修改群基础资料成功");
			
			HashMap<String, String> tags = new HashMap<String, String>();
			tags.put("Tag_Profile_IM_Image", headPic == null?"":headPic);
			boolean flag = portraitSet(accountSig, accountIdentifier, account, tags);
			logger.info("修改个人用户的头像结果：" + flag);
			
			return true;
		} else {
			logger.info("修改群基础资料失敗");
			return false;
		}
	}
	
	private JSONObject getItem(String key, String value){
		JSONObject item = new JSONObject();
		item.put("Key", key);
		item.put("Value", value);
		return item;
	}
	
	private String[] getHeadPic(String headPic){
		String[] result = {"","",""};
		if (headPic == null) {
			return result;
		}
		if (headPic.length() < 64) {
			result[0] = headPic;
		}else if (headPic.length() >= 64 && headPic.length() < 128) {
			result[0] = headPic.substring(0, 64);
			result[1] = headPic.substring(64);
		}else {
			result[0] = headPic.substring(0, 64);
			result[1] = headPic.substring(64, 128);
			result[2] = headPic.substring(128);
		}
		return result;
	}
	
	@Override
	public List<GroupObj> getGroupList(String groupIdentifier, String groupSig, String userIdentifier,int userLevel) {
		/**
		 * {
		  "code": "",
		  "msg": "",
		  "groupList": [
		    {
		      "id": "",
		      "groupName": "",
		      "groupPic": "",
		      "groupId": "",
		      "groupLevel": "",
		      "groupOwner": "",
		      "groupCount": "",
		      "groupLimit": "",
		      "groupMemberList": [
		        {
		          "headPic": "",
		          "identifier": "",
		          "joinTime": "",
		          "nameCard": "",
		          "role": ""
		        }
		      ]
		    }
		  ]
		}		
		 */
		List<GroupObj> groupList = new ArrayList<GroupObj>();
		//每一级别（2.3.4.7）群返回一个未满人的群，如果没有，就创建一个新的群返回
		List<Integer> levelList = new ArrayList<Integer>();
		levelList.add(2);
		levelList.add(3);
		levelList.add(4);
		levelList.add(7);
		
		boolean flag = levelList.contains(new Integer(userLevel));
		
		switch (userLevel) {
		case 2:
			levelList.remove(new Integer(2));
			break;
		case 3:
			levelList.remove(new Integer(3));
			break;
		case 4:
			levelList.remove(new Integer(4));
			break;
		case 7:
			levelList.remove(new Integer(7));
			break;

		default:
			break;
		}
		
		List<String> groupIdList = new ArrayList<String>();
		Map<String, Integer> groupIdLevelMap = new HashMap<String, Integer>();
		
		// 找出用户所在的groupId
		String userGroupId = getUserJoinedGroup(groupIdentifier, groupSig, userIdentifier);
		if (flag && userGroupId == null) {
			// 创建新的群
			int count = getFullGroupIdCountByLevel(userLevel) + 1;
			String groupName = getGroupNameByLevel(userLevel) + String.valueOf(count) + "群";
			userGroupId = createNewGroup(userLevel, groupName, groupIdentifier, groupSig);
			groupIdList.add(userGroupId);
			groupIdLevelMap.put(userGroupId, userLevel);
		}
		// 把其他等级的groupId加入
		for (Integer level : levelList) {
			addGroupIdList(groupIdentifier, groupSig, groupIdList, groupIdLevelMap, level);
		}
		
		// 根据groupIdList找出群信息
		groupList = getGroupInfo(groupIdentifier, groupSig, groupIdList,groupIdLevelMap);
		
		return groupList;
	}
	
	private void addGroupIdList(String groupIdentifier,String groupSig,List<String> groupIdList, Map<String, Integer> groupIdLevel, Integer level) {
		String groupId = getNotFullGroupIdByLevel(level);
		if (groupId == null) {
			// 满群的有多少
			int count = getFullGroupIdCountByLevel(level) + 1;
			// 创建新群
			String groupName = getGroupNameByLevel(level) + String.valueOf(count) + "群";
			groupId = createNewGroup(level, groupName, groupIdentifier, groupSig);
		}
		groupIdLevel.put(groupId, level);
		groupIdList.add(groupId);
	}

	@Override
	public String getUserJoinedGroup(String accountIdentifier,String accountSig,String memberAccount) {
		HashMap<String, Object> jsonmap = new HashMap<>();
		jsonmap.put("Member_Account", memberAccount);
		JSONObject json = JSONObject.parseObject(JSON.toJSONString(jsonmap));
		JSONObject result = HttpClientUtils.Post(GETJOINEDGROUPLIST, json, getGroupParam(accountIdentifier, accountSig));
		/**
{
"ActionStatus": "OK",
"ErrorInfo": "",
"ErrorCode": 0,
"TotalCount": 2, // 不论Limit和Offset如何设置，该值总是满足条件的群组总数
"GroupIdList": [
    {
        "GroupId": "@TGS#2J4SZEAEL"
    },
    {
        "GroupId": "@TGS#2C5SZEAEF"
    }
]
}
		 */
		if (result.containsKey("ActionStatus") && "OK".equals(result.getString("ActionStatus"))) {
			JSONArray array = result.getJSONArray("GroupIdList");
			if (array != null && array.size() > 0) {
				JSONObject item = array.getJSONObject(0);
				return item.getString("GroupId");
			}
		}
		
		return null;
	}
	
	@Override
	public List<GroupObj> getGroupInfo(String accountIdentifier, String accountSig, List<String> groupIdList, Map<String, Integer> groupIdLevel) {
		HashMap<String, Object> jsonmap = new HashMap<>();
		jsonmap.put("GroupIdList", groupIdList);
		JSONObject json = JSONObject.parseObject(JSON.toJSONString(jsonmap));
		JSONObject result = HttpClientUtils.Post(GETGROUPINFO, json, getGroupParam(accountIdentifier, accountSig));
		/**
{
    "ActionStatus":"OK",
    "ErrorCode":0,
    "GroupInfo":[
        {
            "Appid":1400139975,
            "ApplyJoinOption":"NeedPermission",
            "CreateTime":1537329036,
            "ErrorCode":0,
            "FaceUrl":"http://p8i1x61e3.bkt.clouddn.com/2b4a94412732a8b41e2238c1c79c2fe6",
            "GroupId":"@TGS#27QZ72NF5",
            "Introduction":"",
            "LastInfoTime":1537427079,
            "LastMsgTime":1537329115,
            "MaxMemberNum":2000,
            "MemberList":[
                {
                    "AppMemberDefinedData":[
                        {
                            "Key":"member_head_pic",
                            "Value":""
                        },
                        {
                            "Key":"member_head_pic1",
                            "Value":""
                        },
                        {
                            "Key":"member_head_pic2",
                            "Value":""
                        }
                    ],
                    "JoinTime":1537329038,
                    "LastSendMsgTime":0,
                    "Member_Account":"16",
                    "MsgFlag":"AcceptAndNotify",
                    "MsgSeq":1,
                    "NameCard":"匿名",
                    "Role":"Member",
                    "ShutUpUntil":0
                }
            ],
            "MemberNum":5,
            "Name":"TEST高级合伙人1群",
            "NextMsgSeq":3,
            "Notification":"",
            "OnlineMemberNum":0,
            "Owner_Account":"1695",
            "ShutUpAllMember":"Off",
            "Type":"Public"
        }
    ]
}
		 */
		List<GroupObj> groupList = new ArrayList<GroupObj>();
		if (result.containsKey("ActionStatus") && "OK".equals(result.getString("ActionStatus"))) {
			JSONArray array = result.getJSONArray("GroupInfo");
			if (array != null && array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					JSONObject groupItem = array.getJSONObject(i);
					GroupObj group = new GroupObj();
					group.setGroupCount(groupItem.getInteger("MemberNum"));
					group.setGroupId(groupItem.getString("GroupId"));
					group.setGroupLevel(groupIdLevel.get(groupItem.getString("GroupId")));
					group.setGroupLimit(groupItem.getInteger("MaxMemberNum"));
					group.setGroupName(groupItem.getString("Name"));
					group.setGroupOwner(groupItem.getString("Owner_Account"));
					group.setGroupPic(groupItem.getString("FaceUrl"));
					
					List<Map<String, Object>> groupMemberList = new ArrayList<Map<String,Object>>();
					JSONArray memberList = groupItem.getJSONArray("MemberList");
					if (memberList != null) {
						for (int j = 0; j < memberList.size(); j++) {
							JSONObject groupMemberItem = memberList.getJSONObject(j);
							JSONArray appMemberDefinedData = groupMemberItem.getJSONArray("AppMemberDefinedData");
							String headPic = getHeadPicFromData(appMemberDefinedData);
							Map<String, Object> itemMap = new HashMap<String, Object>();
							itemMap.put("headPic", headPic);
							itemMap.put("joinTime", groupMemberItem.getLong("JoinTime"));
							itemMap.put("identifier", groupMemberItem.getString("Member_Account"));
							itemMap.put("nameCard", groupMemberItem.getString("NameCard"));
							itemMap.put("role", groupMemberItem.getString("Role"));
							groupMemberList.add(itemMap);
						}
					}
					
					group.setGroupMemberList(groupMemberList);
					groupList.add(group);
				}
			}
		}
		
		return groupList;
	}

	private String getHeadPicFromData(JSONArray appMemberDefinedData) {
		/**
		 *[
                        {
                            "Key":"member_head_pic",
                            "Value":""
                        },
                        {
                            "Key":"member_head_pic1",
                            "Value":""
                        },
                        {
                            "Key":"member_head_pic2",
                            "Value":""
                        }
                    ]
		 */
		String member_head_pic = "";
		String member_head_pic1 = "";
		String member_head_pic2 = "";
		if (appMemberDefinedData != null) {
			for (int i = 0; i < appMemberDefinedData.size(); i++) {
				JSONObject item = appMemberDefinedData.getJSONObject(i);
				if (item.getString("Key").equals("member_head_pic")) {
					member_head_pic = item.getString("Value");
				}
				if (item.getString("Key").equals("member_head_pic1")) {
					member_head_pic1 = item.getString("Value");
				}
				if (item.getString("Key").equals("member_head_pic2")) {
					member_head_pic2 = item.getString("Value");
				}
			}
		}
		
		return member_head_pic + member_head_pic1 + member_head_pic2;
	}
}
