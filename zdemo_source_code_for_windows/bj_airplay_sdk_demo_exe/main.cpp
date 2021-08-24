#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

#include <string>
#include <sstream>


#include "log.h"
#include "bj_airplay.h"
#include "bj_airplay_callback_imp.h"

#include "licenseapply.h"

char* LEVEL_STR_ARRAY[] = {
	"NOUSE",
	"CRIT",
	"ERROR",
	"WARN",
	"INFO",
	"DEBUG",
	"DETAIL",
	""
};


void defaultTrace(unsigned int level, const char *format, va_list argp)
{
	char szLogBuf[1024] = { '\0' };
	vsnprintf(szLogBuf, 1024, format, argp);

	//if(_cut_level > level)
		//return;
#if 1
    printf("[%s]:%s\n", LEVEL_STR_ARRAY[level], szLogBuf);
#else
	switch(level)
	{
		case PLT_LOG_CRIT:
			__android_log_print(ANDROID_LOG_FATAL, LOG_TAG, "%s", szLogBuf);
			break;
		case PLT_LOG_ERROR:
			__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", szLogBuf);
			break;
		case PLT_LOG_WARN:
			__android_log_print(ANDROID_LOG_WARN, LOG_TAG, "%s", szLogBuf);
			break;
		case PLT_LOG_INFO:
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s", szLogBuf);
			break;
		case PLT_LOG_DEBUG:
			__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s", szLogBuf);
			break;
		case PLT_LOG_DETAIL:
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "%s", szLogBuf);
			break;
		default:
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "%s", szLogBuf);
			break;
	}
#endif
}


int apply_license(const char* mac,const char* user_code)
{
    char saveKey[1024] = {0};
    //输入必捷网络提供的userCode，此处请替换为客户对应的userCode，此userCode为该客户的唯一身份认识，务必记住并做好安全防护，一经丢失或者被盗用将影响客户的正常使用
    const char* userCode = user_code;
    const char* macaddr = mac;  //本机需要绑定的mac地址

    printf("macaddr:%s\n",macaddr);
    int keyLen;

    int getRes = getSecrectKey(NULL,userCode,macaddr,saveKey,1024, &keyLen);
	if (getRes != 0)
	{
		printf("getSecrectKey failed %d\n", getRes);
        return getRes;
	}
	if (keyLen > 0){
		printf("saved key:%s\r\n", saveKey);
		//keyLen>0表示成功，请客户将saveKey字符串保存到文件中或者数据库中维护，务必做好保存
		//若用户重复多次申请License，服务器端将认为是非法请求。

		//后续可从该文件中读取文件内容用于初始化License
		FILE* file = fopen("license.file", "wb");
		if (file)
		{
			fwrite(saveKey, 1, keyLen, file);
			fclose(file);
		}
	}

	//todo
    //web返回lic_type字段为空
    LicInfos lic;
    int rt = GetLicenseInfo(userCode,&lic);
    if(rt == 0)
        printf("license info:%d,%d,%d,%s\n", lic.number, lic.registered, lic.type,lic.expired_date);
    else
        printf("GetLicenseInfo failed ret:%d",rt);
    return 0;
}

int main(int argc, char *argv[])
{
    int apply = 1;
    const char* mac = "00:26:b9:52:7b:e1";
    if(argc > 1)
    {
        mac = argv[1];
    }
    const char* usercode = "";
    if(argc > 2)
    {
        usercode = argv[2];
    }

    if(argc > 3)
    {
        apply = atoi(argv[3]);
    }

    if(apply)
    {
        int ret = apply_license(mac,usercode);
        if(ret)
        {
            printf("apply_license failed mac:%s ret:%d\n",mac,ret);
            return ret;
        }
    }

	AirplayInitPara initpara;
    memset(&initpara,0,sizeof(AirplayInitPara));
    FILE * file = fopen("license.file", "r");
	if (file)
	{
		char key[1024];
		memset(key,0,1024);

		int size = fread(key, 1, 1024, file);
		snprintf(initpara.licenseKey, sizeof(initpara.licenseKey), "%s", key);
		fclose(file);
	}
	else
	{
		printf("oepn license file failed\n");
		return -1;
	}

	strncpy(initpara.friendly_name, "bj_airplay_demo", sizeof(initpara.friendly_name));
	strncpy(initpara.password, "1234", sizeof(initpara.password));
    //strncpy(initpara.itf_name, "eth0", sizeof(initpara.itf_name));
    strncpy(initpara.mac_addr, mac, sizeof(initpara.mac_addr));
    initpara.framerate = 30;
    initpara.resolution_type = BJAir_Resolution_1080P;
    initpara.max_session_nums = 6;
    initpara.airplay_flags = BJ_AIRPLAY_SUPPORT_MBNAUL_FLAG | BJ_AIRPLAY_SUPPORT_SPSPPSNAUL_FLAG;
    initpara.republish_mode = BJAir_RepublishType_Normal;

	initpara.callback.OnStartMirrorPlayer = BJAirplayCallbackImp::OnStartMirrorPlayer;
	initpara.callback.NotifyMirrorAudioCodecInfo = BJAirplayCallbackImp::NotifyMirrorAudioCodecInfo;
	initpara.callback.OnMirrorAudioData = BJAirplayCallbackImp::OnMirrorAudioData;
	initpara.callback.OnMirrorVideoData = BJAirplayCallbackImp::OnMirrorVideoData;
	initpara.callback.OnRotateMirrorVideo = BJAirplayCallbackImp::OnRotateMirrorVideo;
	initpara.callback.OnStopMirrorPlayer = BJAirplayCallbackImp::OnStopMirrorPlayer;
	initpara.callback.RefreshCoverArtFromBuffer = BJAirplayCallbackImp::RefreshCoverArtFromBuffer;
	initpara.callback.SetVolume = BJAirplayCallbackImp::SetVolume;
    initpara.callback.RefreshTrackInfo = BJAirplayCallbackImp::RefreshTrackInfo;

	initpara.callback.OnStartAudioPlayer = BJAirplayCallbackImp::OnStartAudioPlayer;
	initpara.callback.OnAudioData = BJAirplayCallbackImp::OnAudioData;
	initpara.callback.OnStopAudioPlayer = BJAirplayCallbackImp::OnStopAudioPlayer;
	initpara.callback.NotifyAudioCodecInfo = BJAirplayCallbackImp::NotifyAudioCodecInfo;

	initpara.callback.OnStartVideoPlayback = BJAirplayCallbackImp::OnStartVideoPlayback;
	initpara.callback.OnStopVideoPlayback = BJAirplayCallbackImp::OnStopVideoPlayback;
	initpara.callback.OnPauseVideoPlayback = BJAirplayCallbackImp::OnPauseVideoPlayback;
	initpara.callback.OnResumeVideoPlayback = BJAirplayCallbackImp::OnResumeVideoPlayback;
	initpara.callback.OnSeekVideoBySec = BJAirplayCallbackImp::OnSeekVideoBySec;
	initpara.callback.OnGetVideoPositionMSec = BJAirplayCallbackImp::OnGetVideoPositionMSec;
	initpara.callback.OnGetVideoDurationMSec = BJAirplayCallbackImp::OnGetVideoDurationMSec;
    initpara.callback.OnGetVideoPlayerStatus = BJAirplayCallbackImp::OnGetVideoPlayerStatus;

    initpara.callback.OnStartPhotoPlayer = BJAirplayCallbackImp::OnStartPhotoPlayer;
    initpara.callback.OnPlayPhoto = BJAirplayCallbackImp::OnPlayPhoto;
    initpara.callback.OnStopPhotoPlayer = BJAirplayCallbackImp::OnStopPhotoPlayer;

    initpara.callback.OnShowPinCode = BJAirplayCallbackImp::OnShowPinCode;
    initpara.callback.OnVerifyPinSuccess = BJAirplayCallbackImp::OnVerifyPinSuccess;
    initpara.callback.OnProbePlayerAbility = BJAirplayCallbackImp::OnProbePlayerAbility;
    initpara.callback.LogFun = defaultTrace;

	int res = InitBJAirplay(NULL,&initpara);
	if (res)
	{
		TRACE_ERR("InitBJAirplay failed");
	}
    //SetAirplayPassword("8888");
    std::stringstream   ss;
    int pinseed = 1000;
    int i = 0;
    ss<<pinseed++;
    //SetAirplayPassword(ss.str().c_str());
	while (1)
	{
		sleep(1);
        i++;
        if(i == 60)
        {
            ss.str("");
            ss<<pinseed++;
            //SetAirplayPassword(ss.str().c_str());
            i = 0;
        }
	}

	UninitBJAirplay();

	return 0;
}
