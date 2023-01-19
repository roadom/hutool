package cn.hutool.core.text.csv;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.*;
import sun.nio.cs.StreamDecoder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV文件读取器基础类，提供灵活的文件、路径中的CSV读取，一次构造可多次调用读取不同数据，参考：FastCSV
 *
 * @author Looly
 * @since 5.0.4
 */
public class CsvBaseReader implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 默认编码
	 */
	protected static final Charset DEFAULT_CHARSET = CharsetUtil.CHARSET_UTF_8;

	protected static final char DEFAULT_FIELD_SEPARATOR = CharUtil.COMMA;

	private final CsvReadConfig config;

	//--------------------------------------------------------------------------------------------- Constructor start

	/**
	 * 构造，使用默认配置项
	 */
	public CsvBaseReader() {
		this(null);
	}

	/**
	 * 构造
	 *
	 * @param config 配置项
	 */
	public CsvBaseReader(CsvReadConfig config) {
		this.config = ObjectUtil.defaultIfNull(config, CsvReadConfig.defaultConfig());
	}
	//--------------------------------------------------------------------------------------------- Constructor end

	/**
	 * 设置字段分隔符，默认逗号','
	 *
	 * @param fieldSeparator 字段分隔符，默认逗号','
	 */
	public void setFieldSeparator(char fieldSeparator) {
		this.config.setFieldSeparator(fieldSeparator);
	}

	/**
	 * 设置 文本分隔符，文本包装符，默认双引号'"'
	 *
	 * @param textDelimiter 文本分隔符，文本包装符，默认双引号'"'
	 */
	public void setTextDelimiter(char textDelimiter) {
		this.config.setTextDelimiter(textDelimiter);
	}

	/**
	 * 设置是否首行做为标题行，默认false
	 *
	 * @param containsHeader 是否首行做为标题行，默认false
	 */
	public void setContainsHeader(boolean containsHeader) {
		this.config.setContainsHeader(containsHeader);
	}

	/**
	 * 设置是否跳过空白行，默认true
	 *
	 * @param skipEmptyRows 是否跳过空白行，默认true
	 */
	public void setSkipEmptyRows(boolean skipEmptyRows) {
		this.config.setSkipEmptyRows(skipEmptyRows);
	}

	/**
	 * 设置每行字段个数不同时是否抛出异常，默认false
	 *
	 * @param errorOnDifferentFieldCount 每行字段个数不同时是否抛出异常，默认false
	 */
	public void setErrorOnDifferentFieldCount(boolean errorOnDifferentFieldCount) {
		this.config.setErrorOnDifferentFieldCount(errorOnDifferentFieldCount);
	}

	/**
	 * 读取CSV文件，默认UTF-8编码
	 *
	 * @param file CSV文件
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public CsvData read(File file) throws IORuntimeException {
		return read(file, DEFAULT_CHARSET);
	}

	/**
	 * 读取CSV文件
	 *
	 * @param file    CSV文件
	 * @param charset 文件编码，默认系统编码
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public CsvData read(File file, Charset charset) throws IORuntimeException {
		return read(Objects.requireNonNull(file.toPath(), "file must not be null"), charset);
	}

	/**
	 * 读取CSV文件，默认UTF-8编码
	 *
	 * @param path CSV文件
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public CsvData read(Path path) throws IORuntimeException {
		return read(path, DEFAULT_CHARSET);
	}

	/**
	 * 读取CSV文件
	 *
	 * @param path    CSV文件
	 * @param charset 文件编码，默认系统编码
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public CsvData read(Path path, Charset charset) throws IORuntimeException {
		//通过名称判断是否是 tsv，如果是，则修改间隔符配置
		if(StrUtil.endWith(path.getFileName().toString(), ".tsv", true)){
			this.config.setFieldSeparator(CharPool.TAB);
		}
		Assert.notNull(path, "path must not be null");
		return read(FileUtil.getReader(path, charset));
	}

	/**
	 * 从Reader中读取CSV数据，读取后关闭Reader
	 *
	 * @param reader Reader
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public CsvData read(Reader reader) throws IORuntimeException {
		return this.read(reader, DEFAULT_FIELD_SEPARATOR);
	}

	public CsvData read(Reader reader, char fieldSeparator) throws IORuntimeException {
		if(!CharUtil.isBlankChar(fieldSeparator)){
			this.config.setFieldSeparator(fieldSeparator);
		}
		final CsvParser csvParser = parse(reader);
		final List<CsvRow> rows = new ArrayList<>();
		read(csvParser, rows::add);
		final List<String> header = config.containsHeader ? csvParser.getHeader() : null;

		return new CsvData(header, rows);
	}

	/**
	 * 从Reader中读取CSV数据，结果为Map，读取后关闭Reader。<br>
	 * 此方法默认识别首行为标题行。
	 *
	 * @param reader Reader
	 * @return {@link CsvData}，包含数据列表和行信息
	 * @throws IORuntimeException IO异常
	 */
	public List<Map<String, String>> readMapList(Reader reader) throws IORuntimeException {
		return this.readMapList(reader, DEFAULT_FIELD_SEPARATOR);
	}

	public List<Map<String, String>> readMapList(Reader reader, char fieldSeparator) throws IORuntimeException {
		// 此方法必须包含标题
		this.config.setContainsHeader(true);
		if(!CharUtil.isBlankChar(fieldSeparator)){
			this.config.setFieldSeparator(fieldSeparator);
		}
		final List<Map<String, String>> result = new ArrayList<>();
		read(reader, (row) -> result.add(row.getFieldMap()));
		return result;
	}

	/**
	 * 从Reader中读取CSV数据并转换为Bean列表，读取后关闭Reader。<br>
	 * 此方法默认识别首行为标题行。
	 *
	 * @param <T>    Bean类型
	 * @param reader Reader
	 * @param clazz  Bean类型
	 * @return Bean列表
	 */
	public <T> List<T> read(Reader reader, Class<T> clazz) {
		return this.read(reader, clazz, DEFAULT_FIELD_SEPARATOR);
	}
	public <T> List<T> read(Reader reader, Class<T> clazz, char fieldSeparator) {
		// 此方法必须包含标题
		this.config.setContainsHeader(true);
		if(!CharUtil.isBlankChar(fieldSeparator)){
			this.config.setFieldSeparator(fieldSeparator);
		}
		final List<T> result = new ArrayList<>();
		read(reader, (row) -> result.add(row.toBean(clazz)));
		return result;
	}

	/**
	 * 从Reader中读取CSV数据，读取后关闭Reader
	 *
	 * @param reader     Reader
	 * @param rowHandler 行处理器，用于一行一行的处理数据
	 * @throws IORuntimeException IO异常
	 */
	public void read(Reader reader, CsvRowHandler rowHandler) throws IORuntimeException {
		this.read(reader, rowHandler, DEFAULT_FIELD_SEPARATOR);
	}
	public void read(Reader reader, CsvRowHandler rowHandler, char fieldSeparator) throws IORuntimeException {
		if(!CharUtil.isBlankChar(fieldSeparator)){
			this.config.setFieldSeparator(fieldSeparator);
		}
		read(parse(reader), rowHandler);
	}

	//--------------------------------------------------------------------------------------------- Private method start

	/**
	 * 读取CSV数据，读取后关闭Parser
	 *
	 * @param csvParser  CSV解析器
	 * @param rowHandler 行处理器，用于一行一行的处理数据
	 * @throws IORuntimeException IO异常
	 * @since 5.0.4
	 */
	private void read(CsvParser csvParser, CsvRowHandler rowHandler) throws IORuntimeException {
		try {
			CsvRow csvRow;
			while ((csvRow = csvParser.nextRow()) != null) {
				rowHandler.handle(csvRow);
			}
		} finally {
			IoUtil.close(csvParser);
		}
	}

	/**
	 * 构建 {@link CsvParser}
	 *
	 * @param reader Reader
	 * @return CsvParser
	 * @throws IORuntimeException IO异常
	 */
	private CsvParser parse(Reader reader) throws IORuntimeException {

		return new CsvParser(reader, this.config);

	}
	//--------------------------------------------------------------------------------------------- Private method start
}
