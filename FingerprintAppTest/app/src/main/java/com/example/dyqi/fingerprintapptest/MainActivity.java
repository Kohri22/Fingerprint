package com.example.dyqi.fingerprintapptest;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by dyqi on 2018/1/31.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    FingerprintManagerCompat mFingerprintManagerCompat;
    KeyguardManager mKeyguardManager;
    CryptoObjectHelper mCryptoObjectHelper;

    TextView state0, state1, state2, fingerprintState, showtv;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            showtv.setText((String) (msg.obj));
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }


    public void initView() {

        state0 = findViewById(R.id.state0);
        state1 = findViewById(R.id.state1);
        state2 = findViewById(R.id.state2);
        fingerprintState = findViewById(R.id.fingerprintState);
        showtv = findViewById(R.id.showtext);

        try {
            mFingerprintManagerCompat = FingerprintManagerCompat.from(this);
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            mCryptoObjectHelper = new CryptoObjectHelper();

            if (checkForFingerprint()) {
                fingerprintState.setText("可以使用指纹识别模块");
            } else {
                fingerprintState.setText("不可使用指纹识别模块");
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "没有指纹识别权限", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (Exception e) {
            handler.hasMessages(1, e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean checkForFingerprint() {
        boolean canUseFingerprint = true;

        /**
         * 指纹识别肯定要求你的设备上有指纹识别的硬件，因此在运行时需要检查系统当中是不是有指纹识别的硬件
         */
        if (!mFingerprintManagerCompat.isHardwareDetected()) {
            state0.setText("已检测到指纹识别设备");
        } else {
            state0.setText("未检测到指纹识别设备，不可测试指纹识别");
            canUseFingerprint = false;
        }

        /**
         * 当前设备必须是处于安全保护中的
         * 这个条件的意思是，你的设备必须是使用屏幕锁保护的，这个屏幕锁可以是password，PIN或者图案都行。
         * 为什么是这样呢？因为google原生的逻辑就是：想要使用指纹识别的话，必须首先使能屏幕锁才行，这个和android 5.0中的smart lock逻辑是一样的，
         * 这是因为google认为目前的指纹识别技术还是有不足之处，安全性还是不能和传统的方式比较的。
         */
        if (mKeyguardManager.isKeyguardSecure()) {
            state1.setText("当前设备处于安全保护中");
        } else {
            state1.setText("当前设备未处于安全保护中，不可测试指纹识别");
            canUseFingerprint = false;
        }

        /**
         * 系统中是不是有注册的指纹
         * 在android 6.0中，普通app要想使用指纹识别功能的话，用户必须首先在setting中注册至少一个指纹才行，否则是不能使用的。
         * 所以这里我们需要检查当前系统中是不是已经有注册的指纹信息了：
         */
        if (!mFingerprintManagerCompat.hasEnrolledFingerprints()) {
            state2.setText("已检测到setting中存在指纹");
        } else {
            state2.setText("尚未检测到setting中存在指纹");
            canUseFingerprint = false;
        }
        return canUseFingerprint;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.submit:
                Log.i("TAG", " mFingerprintManagerCompat.authenticate");
                try {
                    mFingerprintManagerCompat.authenticate(
                            mCryptoObjectHelper.buildCryptoObject(),
                            0,
                            new CancellationSignal(),
                            new FingerprintManagerCompat.AuthenticationCallback() {
                                @Override
                                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                                    Log.i("TAG", "onAuthenticationError");
                                    handler.hasMessages(errMsgId, errMsgId + "\n" + errString.toString());
                                }

                                @Override
                                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                                    Log.i("TAG", "onAuthenticationHelp");
                                    handler.hasMessages(helpMsgId, helpMsgId + "\n" + helpString.toString());
                                }

                                @Override
                                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                                    Log.i("TAG", "onAuthenticationSucceeded");
                                    handler.hasMessages(0, result.getCryptoObject().getSignature());
                                }

                                @Override
                                public void onAuthenticationFailed() {
                                    Log.i("TAG", "onAuthenticationFailed");
                                    handler.hasMessages(1, "认证失败");
                                }
                            }, null);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
        }
    }
}
