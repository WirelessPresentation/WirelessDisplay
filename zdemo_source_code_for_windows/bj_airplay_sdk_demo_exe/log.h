#ifndef _AVN_FRAMEWORK_LOG_H
#define _AVN_FRAMEWORK_LOG_H

#include <stdarg.h>

enum {
	VOS_LOG_NOUSE = 0,
	VOS_LOG_CRIT,
	VOS_LOG_ERROR,
	VOS_LOG_WARN,
	VOS_LOG_INFO,
	VOS_LOG_DEBUG,
	VOS_LOG_DETAIL,
	VOS_LOG_MAX,
};

/*#define AVN_FWK_BASIC_TRACE(level, format, ...)   do{\
avn_fwk_trace(level, "%s(%d): "format"", __FILE__, __LINE__, ##__VA_ARGS__); \
}while(0)*/

#define AVN_FWK_BASIC_TRACE(level, format, ...)   do{\
	avn_fwk_trace(level, format, ##__VA_ARGS__); \
}while(0)


#define TRACE_CRIT(format, ...) AVN_FWK_BASIC_TRACE(VOS_LOG_CRIT,format,##__VA_ARGS__)
#define TRACE_ERR(format, ...)  AVN_FWK_BASIC_TRACE(VOS_LOG_ERROR,format,##__VA_ARGS__)
#define TRACE_WARN(format, ...) AVN_FWK_BASIC_TRACE(VOS_LOG_WARN,format,##__VA_ARGS__)
#define TRACE_INFO(format, ...) AVN_FWK_BASIC_TRACE(VOS_LOG_INFO,format,##__VA_ARGS__)
#define TRACE_DBG(format, ...)  AVN_FWK_BASIC_TRACE(VOS_LOG_DEBUG,format,##__VA_ARGS__)
#define TRACE_DTL(format, ...)  AVN_FWK_BASIC_TRACE(VOS_LOG_DETAIL,format,##__VA_ARGS__)
#define TRACE_ASSERT(format, ...)  AVN_FWK_BASIC_TRACE(VOS_LOG_DETAIL,format,##__VA_ARGS__)

void avn_fwk_trace(unsigned int level, const char *format, ...);
//void set_fwk_tracefun(AvnLogFun_T fun);

#endif
