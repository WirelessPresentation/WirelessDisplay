#include "log.h"

#include <stdio.h>
#include <vector>
#include <functional>

#include <iostream>
#include <fstream>

typedef std::function<void(unsigned int level, const char *format, va_list argp)> AvnLogFun_T;

namespace {
	class Logger
	{
	public:
		Logger();
		void reg_tracefun(AvnLogFun_T fun);
		void trace(unsigned int level, const char *format, va_list argp);
		void defaultTrace(unsigned int level, const char *format, va_list argp);
	private:
		AvnLogFun_T _logFun;
		std::ofstream ofs;
	};

	void Logger::reg_tracefun(AvnLogFun_T fun)
	{
		_logFun = fun;
	}

	void Logger::trace(unsigned int level, const char *format, va_list argp)
	{
		if (_logFun)
		{
			_logFun(level, format, argp);
		}
	}

	Logger::Logger()
	{
		_logFun = std::bind(&Logger::defaultTrace, this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
		//ofs.open("test.log", std::ios_base::out);
	}

	void Logger::defaultTrace(unsigned int level, const char *format, va_list argp)
	{
		char szLogBuf[1024] = { '\0' };
		vsnprintf(szLogBuf, 1024, format, argp);

		//if (ofs.is_open())
		//{
			//ofs << szLogBuf << std::endl;
		//}

	    //std::cout << szLogBuf << std::endl;
		printf("%s\n", szLogBuf);

		//::trace(rootLogPtr,level,format,argp);
	}

	static Logger logger;
}

void avn_fwk_trace(unsigned int level, const char *format, ...)
{
	va_list argp;
	va_start(argp, format);
	::logger.trace(level, format, argp);
	va_end(argp);
}

void set_fwk_tracefun(AvnLogFun_T fun)
{
	::logger.reg_tracefun(fun);
}

