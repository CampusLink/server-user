package com.campus.user;

import com.campus.system.ServiceContext;
import com.campus.system.ServiceMenu;
import com.campus.system.annotation.Service;
import com.campus.system.storage.StorageService;
import com.campus.system.storage.model.Box;
import com.campus.system.storage.model.BoxStore;
import com.campus.system.storage.model.StorageType;
import com.campus.system.token.TokenService;
import com.campus.system.token.model.Token;
import com.campus.system.user.UserService;
import com.campus.system.user.model.OrgReq;
import com.campus.system.user.model.User;
import com.campus.user.manager.AuthCodeManager;
import com.oracle.tools.packager.Log;

import java.util.List;
@Service(name = ServiceMenu.USER, module = "User")
public class UserServiceImpl extends UserService {
    private StorageService mStorageService;
    private TokenService mTokenService;
    private Box<User> mUserBox;
    private Box<OrgReq> mOrgReqBox;
    public void init(ServiceContext serviceContext) {
        mTokenService = (TokenService) serviceContext.getSystemService(ServiceMenu.TOKEN);
        mStorageService = (StorageService) serviceContext.getSystemService(ServiceMenu.STORAGE);
        BoxStore boxStore = mStorageService.obtainBoxStore(StorageType.MySql, "username", "password");
        mUserBox = boxStore.boxFor(User.class);
        mOrgReqBox = boxStore.boxFor(OrgReq.class);
        AuthCodeManager.getInstance().init(serviceContext);
    }

    public User loginByPhoneAndAuthCode(String phone, String code) {
        if(!AuthCodeManager.getInstance().verifyPhoneAndCode(phone, code)){
            return null;
        }
        List<User> users = mUserBox.obtainQuery().whereEqualTo("phone", phone).limit(1).query();
        User user;
        if(users == null || users.size() == 0){
            //此用户还未注册
            user = registerByPhoneAndAuthCode(phone, code);
        }else{
            user = users.get(0);
        }
        user.setPassword("");
        return user;
    }

    public User registerByPhoneAndAuthCode(String phone, String code) {
        User user = new User();
        user.setPhone(phone);
        user.setUserId(System.currentTimeMillis() + code + (int)(Math.random() * 1000));
        mUserBox.put(user);
        return user;
    }

    public void initLoginPassword(String tokenStr, String password) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        user.setPassword(password);
        mUserBox.put(user);
    }

    public void resetLoginPassword(String tokenStr, String password) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        user.setPassword(password);
        mUserBox.put(user);
    }

    public void sendLoginAuthCode(String phone, String deviceId) {
        AuthCodeManager.getInstance().sendAuthCode(phone, deviceId);
    }

    public User queryUserDescById(String tokenStr, String userId) {
        Token token = mTokenService.parseToken(tokenStr);
        String fromId = token.getUserId();
        Log.debug(fromId + "的用户请求" + userId + "用户的简单信息");
        List<User> users = mUserBox.obtainQuery().whereEqualTo("userId", userId).limit(1).query();
        if(users == null || users.size() == 0){
            return null;
        }
        User user = users.get(0);
        user.setPassword("");
        user.setPhone("");
        user.setBirth(0);
        user.setOrgDesc("");
        user.setOrgId("");
        user.setSex(-1);
        user.setSign("");
        user.setID(0);
        return user;
    }

    public User queryUserInfoById(String tokenStr, String userId) {
        Token token = mTokenService.parseToken(tokenStr);
        String fromId = token.getUserId();
        Log.debug(fromId + "的用户请求" + userId + "用户的简单信息");
        List<User> users = mUserBox.obtainQuery().whereEqualTo("userId", userId).limit(1).query();
        if(users == null || users.size() == 0){
            return null;
        }
        User user = users.get(0);
        user.setPassword("");
        user.setPhone("");
        user.setID(0);
        return user;
    }

    public void resetUserHead(String tokenStr, String head) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        user.setHead(head);
        mUserBox.put(user);
    }

    public void resetUserNickName(String tokenStr, String nickName) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        user.setNickName(nickName);
        mUserBox.put(user);
    }

    public void resetUserSign(String tokenStr, String sign) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        user.setSign(sign);
        mUserBox.put(user);
    }

    public void resetUserLoginPhone(String resetPhoneToken, String phone, String authCode) {

    }

    /**
     * 重新设置手机号
     * @param token 用户token
     * @param passwords 历史密码，按顺序 由新到旧
     * @return
     */
    public String requestResetLoginPhoneToken(String token, String... passwords) {
        return null;
    }

    public void requestResetUserOrgInfo(String tokenStr, String orgId, String orgDesc) {
        User user = asyncUserInfo(tokenStr);
        if(user == null){
            return;
        }
        OrgReq req = new OrgReq();
        req.setCreateTime(System.currentTimeMillis());
        req.setOrgDesc(orgDesc);
        req.setOrgId(orgId);
        req.setUserId(user.getUserId());
        req.setStatus(OrgReq.OrgReqStatus.NONE);
        req.setOrgReqId(System.currentTimeMillis() + "" + (int)(Math.random() * 1000));
        mOrgReqBox.put(req);
    }

    public List<OrgReq> queryOrgReqListByAdmin(String token, String preId, int pageSize) {
        User user = asyncUserInfo(token);
        //判断此User是否是管理员
        List<OrgReq> orgReqs = mOrgReqBox.obtainQuery().whereEqualTo("status", OrgReq.OrgReqStatus.NONE).limit(pageSize).query();
        return orgReqs;
    }

    public void operateOrgReqByAdmin(String token, String orgReqId, boolean agree) {
        User user = asyncUserInfo(token);
        //TODO 判断此User是否是管理员
        List<OrgReq> orgReqs = mOrgReqBox.obtainQuery().whereEqualTo("orgReqId", orgReqId).limit(1).query();
        if(orgReqs == null || orgReqs.size() == 0){
            return;
        }

        OrgReq req = orgReqs.get(0);
        req.setStatus(agree ? OrgReq.OrgReqStatus.AGREE : OrgReq.OrgReqStatus.DISAGREE);
        mOrgReqBox.put(req);
    }

    public User asyncUserInfo(String tokenStr) {
        Token token = mTokenService.parseToken(tokenStr);
        String userId = token.getUserId();
        List<User> users = mUserBox.obtainQuery().whereEqualTo("userId", userId).limit(1).query();
        if(users == null || users.size() == 0){
            return null;
        }
        User user = users.get(0);
        return user;
    }
}