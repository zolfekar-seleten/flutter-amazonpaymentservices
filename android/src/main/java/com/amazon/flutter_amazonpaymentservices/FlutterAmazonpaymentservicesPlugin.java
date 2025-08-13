package com.amazon.flutter_amazonpaymentservices;

import static android.app.Activity.RESULT_OK;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import android.app.Activity;
import android.content.Intent;

import com.payfort.fortpaymentsdk.FortSdk;
import com.payfort.fortpaymentsdk.callbacks.FortCallBackManager;
import com.payfort.fortpaymentsdk.callbacks.PayFortCallback;
import com.payfort.fortpaymentsdk.callbacks.FortInterfaces;
import com.payfort.fortpaymentsdk.domain.model.FortRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * FlutterAmazonpaymentservicesPlugin
 */
public class FlutterAmazonpaymentservicesPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private static final String METHOD_CHANNEL_KEY = "flutter_amazonpaymentservices";
    private static final int PAYFORT_REQUEST_CODE = 1166;

    private MethodChannel methodChannel;
    private static Activity activity;
    static FortCallBackManager fortCallback;
    private Constants.ENVIRONMENTS_VALUES mEnvironment;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel = new MethodChannel(binding.getBinaryMessenger(), METHOD_CHANNEL_KEY);
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener((requestCode, resultCode, data) -> {
            if (requestCode == PAYFORT_REQUEST_CODE) {
                if (data != null && resultCode == RESULT_OK) {
                    fortCallback.onActivityResult(requestCode, resultCode, data);
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("", "");
                    fortCallback.onActivityResult(requestCode, resultCode, intent);
                }
            }
            return true;
        });
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "normalPay":
                handleOpenFullScreenPayfort(call, result);
                break;
            case "getUDID":
                result.success(FortSdk.getDeviceId(activity));
                break;
            case "validateApi":
                handleValidateAPI(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void handleValidateAPI(MethodCall call, MethodChannel.Result result) {
        if ("production".equals(call.argument("environmentType"))) {
            mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
        } else {
            mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
        }

        HashMap<String, Object> requestParamMap = call.argument("requestParam");
        FortRequest fortRequest = new FortRequest();
        fortRequest.setRequestMap(requestParamMap);

        FortSdk.getInstance().validate(activity, FortSdk.ENVIRONMENT.TEST, fortRequest, new PayFortCallback() {
            @Override
            public void startLoading() {}

            @Override
            public void onSuccess(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
                result.success(fortResponseMap);
            }

            @Override
            public void onFailure(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
                result.error("onFailure", "onFailure", fortResponseMap);
            }
        });
    }

    private void handleOpenFullScreenPayfort(MethodCall call, MethodChannel.Result result) {
        try {
            if ("production".equals(call.argument("environmentType"))) {
                mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
            } else {
                mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
            }

            boolean isShowResponsePage = call.argument("isShowResponsePage");
            HashMap<String, Object> requestParamMap = call.argument("requestParam");
            FortRequest fortRequest = new FortRequest();
            fortRequest.setShowResponsePage(isShowResponsePage);
            fortRequest.setRequestMap(requestParamMap);

            if (fortCallback == null) {
                fortCallback = FortCallBackManager.Factory.create();
            }

            FortSdk.getInstance().registerCallback(activity, fortRequest, mEnvironment.getSdkEnvironemt(), PAYFORT_REQUEST_CODE, fortCallback, true, new FortInterfaces.OnTnxProcessed() {
                @Override
                public void onCancel(Map<String, Object> requestParamsMap, Map<String, Object> responseMap) {
                    result.error("onCancel", "onCancel", responseMap);
                }

                @Override
                public void onSuccess(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
                    result.success(fortResponseMap);
                }

                @Override
                public void onFailure(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
                    result.error("onFailure", "onFailure", fortResponseMap);
                }
            });
        } catch (Exception e) {
            HashMap<Object, Object> errorDetails = new HashMap<>();
            errorDetails.put("response_message", e.getMessage());
            result.error("onFailure", "onFailure", errorDetails);
        }
    }
}
