#ifndef _LCTRLAPPLY_CTLR_XX_
#define _LCTRLAPPLY_CTLR_XX_

#ifdef LCTRLAPPLYEXPORTS
#define LCTRLAPPLY _declspec(dllexport)
#else
#define LCTRLAPPLY __attribute__((visibility ("default")))
#endif

#include <jni.h>

typedef struct
{
	unsigned int number;           //number
	unsigned int registered;	   //already registered number
	unsigned int type;			   //license type used for product
	char expired_date[16];		   //expiry date
}LicInfos;


#define IN
#define OUT
#define INOUT

#ifdef __cplusplus
extern "C"{
#endif

//user apply for secret key on specific machine

/**
* \brief        get the generated secret key.
* \param userCode  provided by bijie; only one for each SDK.
* \param deviceId    provided by user. the mac addr of device
* \param licenseKey    the license key. IN/OUT
* \param licenseKeyMaxLen    the license key max len. set it as 1024 is recommended.
* \param LicenseKeyLen    the length of license key.
* \return       int
*/
LCTRLAPPLY int getSecrectKey(IN JNIEnv* env, IN const char* userCode, IN const char* deviceId, INOUT char* licenseKey, IN int licenseKeyMaxLen, OUT int* licenseKeyLen);


/**
* \brief				apply the license key for V3.
* \param userCode       provided by bijie; only one for each SDK.
* \param licenseNO      provided by bijie; the license serial number, each num can only bind one deviceId.
* \param deviceId       provided by user.
* \param proGuardMode   provided by user.
* \param proGuardSalt   provided by user.
* \param licenseKey   provided by user, if applied succeed the result of license key will be set to this param.
* \param maxLen   the max length of licenseKey param.
* \param keyLen   the real length of licenseKey if applied succeed.
* \param serverUrl   the server url. if not set default is  public net "license.bijienetworks.com" .  if need set to intranlet for test, set it as 192.168.10.63:8080
* \return       int
*/
LCTRLAPPLY int ApplyLicenseKeyWithLicenseNO(IN JNIEnv* env, IN const char* userCode, IN const char* licenseNO, IN const char* deviceId, IN int proGuardMode, IN const char* proGuardSalt, INOUT char* licenseKey, IN int maxLen, OUT int* keyLen);

/**
* \brief				apply the license key for V1 & V2.
* \param userCode       provided by bijie; only one for each SDK.
* \param deviceId       provided by user.
* \param proGuardMode   provided by user.
* \param proGuardSalt   provided by user.
* \param licenseKey   provided by user, if applied succeed the result of license key will be set to this param.
* \param maxLen   the max length of licenseKey param.
* \param keyLen   the real length of licenseKey if applied succeed.
* \param serverUrl   the server url. if not set default is  public net "license.bijienetworks.com" .  if need set to intranlet for test, set it as 192.168.10.63:8080
* \return       int
*/
LCTRLAPPLY int ApplyLicenseKeyWithoutLicenseNO(IN JNIEnv* env, IN const char* userCode, IN const char* deviceId, IN int proGuardMode, IN const char* proGuardSalt, INOUT char* licenseKey, IN int maxLen, OUT int* keyLen);


//user query for license info on specific userCode

/**
* \brief        query license info.
* \param userCode  provided by bijie; only one for each SDK.
* \param info  the licenseKey info.
* \param serverUrl   the server url. if not set default is  public net "license.bijienetworks.com" .  if need set to intranlet for test, set it as 192.168.10.63:8080
* \return       int
*/
LCTRLAPPLY int GetLicenseInfo(IN const char* userCode, INOUT LicInfos* info);


#ifdef __cplusplus
}
#endif

#endif
