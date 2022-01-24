package com.harris.inxcamerax.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.harris.inxcamerax.CameraXActivity;

import java.io.File;


/**
 * author Created by harrishuang on 6/28/21.
 * email : huangjinping1000@163.com
 */
public class CameraParam implements Parcelable {
    private boolean front;//相机方向,试试前置摄像头
    private String picturePath;//最终保存路径
    private String pictureTempPath;//拍照的临时路径
    private boolean showMask;//是否显示裁剪框
    private boolean showSwitch;//是否显示切换相机正反面按钮

    private String focusSuccessTips;//聚焦成功提示
    private String focusFailTips;//聚焦失败提示
    private Activity mActivity;//

    private boolean showFocusTips;//是否显示聚焦成功的提示

    private int requestCode;
    private int maskImageId;

    private CameraParam(Builder mBuilder) {
        front = mBuilder.front;
        picturePath = mBuilder.picturePath;
        showMask = mBuilder.showMask;
        showSwitch = mBuilder.showSwitch;
        focusSuccessTips = mBuilder.focusSuccessTips;
        focusFailTips = mBuilder.focusFailTips;
        mActivity = mBuilder.mActivity;
        showFocusTips = mBuilder.showFocusTips;
        requestCode = mBuilder.requestCode;
        maskImageId = mBuilder.maskImageId;
        if (mActivity == null) {
            throw new NullPointerException("Activity param is null");
        }
    }

    protected CameraParam(Parcel in) {
        front = in.readByte() != 0;
        picturePath = in.readString();
        pictureTempPath = in.readString();
        showMask = in.readByte() != 0;
        showSwitch = in.readByte() != 0;
        focusSuccessTips = in.readString();
        focusFailTips = in.readString();
        showFocusTips = in.readByte() != 0;
        requestCode = in.readInt();
        maskImageId = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (front ? 1 : 0));
        dest.writeString(picturePath);
        dest.writeString(pictureTempPath);
        dest.writeByte((byte) (showMask ? 1 : 0));
        dest.writeByte((byte) (showSwitch ? 1 : 0));
        dest.writeString(focusSuccessTips);
        dest.writeString(focusFailTips);
        dest.writeByte((byte) (showFocusTips ? 1 : 0));
        dest.writeInt(requestCode);
        dest.writeInt(maskImageId);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Creator<CameraParam> CREATOR = new Creator<CameraParam>() {
        @Override
        public CameraParam createFromParcel(Parcel in) {
            return new CameraParam(in);
        }

        @Override
        public CameraParam[] newArray(int size) {
            return new CameraParam[size];
        }
    };

    private CameraParam startActivity(int requestCode) {
        Intent intent = new Intent(mActivity, CameraXActivity.class);
        intent.putExtra(CameraConstant.CAMERA_PARAM_KEY, this);
        mActivity.startActivityForResult(intent, requestCode);
        return this;
    }


    public boolean isFront() {
        return front;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public boolean isShowMask() {
        return showMask;
    }

    public boolean isShowSwitch() {
        return showSwitch;
    }


    public String getPictureTempPath() {
        File file = new File(getPicturePath());
        String pictureName = file.getName();
        String newName = null;
        if (pictureName.contains(".")) {
            int lastDotIndex = pictureName.lastIndexOf('.');
            newName = pictureName.substring(0, lastDotIndex) + "_temp" + pictureName.substring(lastDotIndex);
        }
        if (newName == null) {
            newName = pictureName;
        }
        return file.getParent() + File.separator + newName;
    }


    public boolean isShowFocusTips() {
        return showFocusTips;
    }

    public int getRequestCode() {
        return requestCode;
    }


    public static class Builder {
        private boolean front = false;
        private String picturePath = Tools.getPicturePath();
        private boolean showMask = true;
        private boolean showSwitch = false;
        private String focusSuccessTips;//聚焦成功提示
        private String focusFailTips;//聚焦失败提示
        private Activity mActivity;//
        private boolean showFocusTips = true;
        private int requestCode = CameraConstant.REQUEST_CODE;

        private int maskImageId = 0;


        public Builder setMaskImageId(int maskImageId) {
            this.maskImageId = maskImageId;
            return this;
        }

        public Builder setFront(boolean front) {
            this.front = front;
            return this;
        }

        public Builder setPicturePath(String picturePath) {
            this.picturePath = picturePath;
            return this;
        }

        public Builder setShowMask(boolean showMask) {
            this.showMask = showMask;
            return this;
        }

        public Builder setShowSwitch(boolean showSwitch) {
            this.showSwitch = showSwitch;
            return this;
        }


        public Builder setFocusSuccessTips(String focusSuccessTips) {
            this.focusSuccessTips = focusSuccessTips;
            return this;
        }

        public Builder setFocusFailTips(String focusFailTips) {
            this.focusFailTips = focusFailTips;
            return this;
        }

        public Builder setActivity(Activity mActivity) {
            this.mActivity = mActivity;
            return this;
        }

        public Builder setShowFocusTips(boolean showFocusTips) {
            this.showFocusTips = showFocusTips;
            return this;
        }

        public Builder setRequestCode(int requestCode) {
            this.requestCode = requestCode;
            return this;
        }

        public CameraParam build() {
            return new CameraParam(this).startActivity(requestCode);
        }
    }

}
