package com.xiaoyu.app.web.validation.core;
/**
 * @author mapc 
 * @date 2017年4月19日
 */

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.xiaoyu.app.web.validation.annotation.ParamsCheck;
import com.xiaoyu.app.web.validation.annotation.ValidatorName;
import com.xiaoyu.app.web.validation.validator.ParamsValidator;


public class ValidatorContext {
	
	private final static String VALIDATOR_PATH = ParamsValidator.class.getName();
	
	private static String ROOT_PACKAGE_PATH;
	
	private static Map<String,ParamsValidator> validatorMap;
	
	private void initValidator() {
		if (validatorMap == null) {
			validatorMap = new HashMap<String, ParamsValidator>();
			loadValidator();
		}
		return;
	}
	
	public boolean isOk(Method method, Object[] args) throws Exception {
		initValidator();
		if (validatorMap == null || validatorMap.keySet().size() == 0) {
			return false;
		}
		Parameter[] params = method.getParameters();
		boolean isOk = true;
		int index = 0;
		for (Parameter param : params) {
			ParamsCheck paramsCheck = param.getAnnotation(ParamsCheck.class);
			if (paramsCheck == null) {
				continue;
			}
			// 具体策略名称
			String[] validatorNames = paramsCheck.validators();
			// 根据策略名称获取具体策略
			for (String validatorName : validatorNames) {
				ParamsValidator paramsValidator = validatorMap.get(validatorName);
				if (paramsValidator != null) {
					isOk = paramsValidator.checkParams(args[index]);
					if (!isOk) {
						break;
					}
				} else {
					throw new Exception("not find validator : " + validatorName);
				}
			}
			index ++;
		}
		return isOk;
	}
	
	
	private void loadValidator() {
		// 获取所有策略
		try {
			Class<?> clazz = Class.forName(VALIDATOR_PATH);
			if (clazz != null && clazz.isInterface()) {
				// 根包路径
				ROOT_PACKAGE_PATH = clazz.getResource("/").getPath();
				loadValidatorByFile(new File(ROOT_PACKAGE_PATH));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void loadValidatorByFile(File file) {
		if (!file.isDirectory()) {
			try {
				if (file.getName().endsWith(".class")) {
					if (file.getAbsolutePath().replaceAll(ROOT_PACKAGE_PATH, "").length() < file.getAbsolutePath().length()) {
						String classPath = file.getAbsolutePath().replaceAll(ROOT_PACKAGE_PATH, "").replace("/", ".");
						Class<?> clazz = Class.forName(classPath.substring(0, classPath.length() - 6));
						
						if (!clazz.isInterface() && isImplementor(ParamsValidator.class, clazz)) {
							ParamsValidator paramsValidator = (ParamsValidator) clazz.newInstance();
							String validatorName = paramsValidator.getClass().getAnnotation(ValidatorName.class).value();
							validatorMap.put(validatorName, paramsValidator);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			for (File f : file.listFiles()) {
				loadValidatorByFile(f);
			}
		}
	}
	
	private boolean isImplementor(Class<ParamsValidator> inter, Class<?> implementor) {
		Class<?>[] classes = implementor.getInterfaces();
		for (Class<?> c : classes) {
			if (c.equals(inter)) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) {
		ValidatorContext validatorContext = new ValidatorContext();
		validatorContext.initValidator();
		System.out.println("validators implementor is : " + ValidatorContext.validatorMap);
	}
	
}
