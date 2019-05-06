package com.campus.user.manager;

import com.campus.system.ServiceContext;

public class AuthCodeManager {
    private ServiceContext mContext;
    public static AuthCodeManager getInstance(){
        return Holder.sIntance;
    }

    private static class Holder{
        static AuthCodeManager sIntance = new AuthCodeManager();
    }

    private AuthCodeManager() {
    }

    public synchronized void init(ServiceContext context){
        if(mContext != null){
            return;
        }
        mContext = context;
    }

    public void sendAuthCode(String phone, String deviceId){

    }

    public boolean verifyPhoneAndCode(String phone, String code){
        return true;
    }
}
