package com.fd.service.community.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fd.provide.community.GroupService;
import com.fd.provide.community.SigService;
import com.tls.sigcheck.util.ImSigUtil;

@Service("sigService")
public class SigServiceImpl implements SigService {
	private static final Logger logger = LoggerFactory.getLogger(SigServiceImpl.class);

	@Value("${im.sdkAppId}")
	private String sdkAppId;
    
    @Value("${im.admin}")
    private String admin;
	
	@Value("${im.admin}")
    private String admin;
	
	@Value("${im.admin}")
    private String bb;
	
	@Autowired
	private GroupService groupService;

	@Override
	public String getUserSig(String identifier,boolean needRegister) {
		try {
			String sig = ImSigUtil.getSig(sdkAppId, identifier);
			if (needRegister) {
				// 注册
				groupService.register(ImSigUtil.getSig(sdkAppId, admin), admin, identifier);
			}
			return sig;
		} catch (Exception e) {
			logger.error("获取用户sig出错" + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean checkUserSig(String identifier, String sig) {
		try {
			return ImSigUtil.checkSig(sdkAppId, sig, identifier);
		} catch (Exception e) {
			logger.error("检验用户sig出错" + e.getMessage());
		}
		return false;
	}
	
		@Override
	public String getUserSig(String identifier,boolean needRegister) {
		try {
			String sig = ImSigUtil.getSig(sdkAppId, identifier);
			if (needRegister) {
				// 注册
				groupService.register(ImSigUtil.getSig(sdkAppId, admin), admin, identifier);
			}
			return sig;
		} catch (Exception e) {
			logger.error("获取用户sig出错" + e.getMessage());
		}
		return null;
	}



}
