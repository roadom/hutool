package cn.hutool.core.text.csv;

import java.io.Serializable;

public class ConverterOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	private HeaderCaseEunm headerCaseEunm = HeaderCaseEunm.DEFAULT;

	public ConverterOptions(){}

	public ConverterOptions(HeaderCaseEunm headerCaseEunm){
		this.headerCaseEunm = headerCaseEunm;
	}

	public HeaderCaseEunm getHeaderCaseEunm() {
		return headerCaseEunm;
	}

	public void setHeaderCaseEunm(HeaderCaseEunm headerCaseEunm) {
		this.headerCaseEunm = headerCaseEunm;
	}


	public enum HeaderCaseEunm {
		//不进行表头大小写转换，直接认为表头可以与字段名匹配。
		DEFAULT,
		//在匹配对账名之前，先尝试将表头从下划线风格转为驼峰风格，并保证首字母小写。
		UNDERLINE_2_CAMEL;

	}
}
