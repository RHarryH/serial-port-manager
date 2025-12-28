package com.navigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
	
	private String userHomeFolder = System.getProperty("user.home");
	private String className, logName;
	
	private Logger(String logName) {
		DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		
		String finalLogName = logName + "_" + timeStampPattern.format(LocalDateTime.now()) + ".log";
		File file = new File(userHomeFolder + "/Desktop/" + finalLogName);

		if (!Files.exists(Paths.get(file.getParent())))
			if(!file.getParentFile().mkdirs())
				System.out.println("Directory creation failed");
		
		this.logName = finalLogName;
	}
	
	public Logger(Class<?> c, String logName) {
		this(logName);
		this.className = c.getSimpleName();
	}
	
	public Logger(String className, String logName) {
		this(logName);
		this.className = className;
	}
	
	public void info(String text) {
		try(FileWriter fw = new FileWriter(userHomeFolder + "/Desktop/" + logName, true);
    	    BufferedWriter bw = new BufferedWriter(fw);
    	    PrintWriter out = new PrintWriter(bw))
    	{
    	    if(className.isEmpty())
    	    	out.println("[" + LocalDateTime.now() + "]: "+ text);
    	    else 
    	    	out.println(className + "[" + LocalDateTime.now() + "]: " + text);
    	} catch (IOException e) {
    	    System.err.println(e);
    	}
	}
}
