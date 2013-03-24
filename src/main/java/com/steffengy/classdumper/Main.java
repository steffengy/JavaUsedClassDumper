package com.steffengy.classdumper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;

public class Main 
{
	public static void main(String[] args)
	{
		if(args.length != 1 && args.length != 2)
			System.out.println(".jar InputJar (OutputJar)");
		else
		{
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			String output = null;
			if(args.length == 2)
			{
				output = args[1];
			}
			try {
				List<String> classes = processJar(args[0]);
				if(output == null)
					for(String str : classes)
						System.out.println(str);
				else
					FileUtils.writeLines(new File(output), classes);
				stopWatch.stop();
				System.out.println(classes.size() + " entrys in " + stopWatch.getTime() + "ms");
			} catch (IOException e) {
				System.out.println("Error");
				e.printStackTrace();
			}
		}
	}
	
	private static List<String> processClass(String inFile, String entryName) throws ClassFormatException, IOException 
	{
		List<String> clses = new ArrayList<String>();
		ClassParser parser = new ClassParser(inFile, entryName);
		JavaClass javaClass = parser.parse();
		Constant[] constantPool = javaClass.getConstantPool().getConstantPool();
		for(Constant constant : constantPool)
		{
			if(constant instanceof ConstantClass)
			{
				ConstantClass cls = (ConstantClass) constant;
				ConstantUtf8 utf8 = (ConstantUtf8) constantPool[cls.getNameIndex()];
				clses.add(utf8.getBytes());
			}
		}
		return clses;
	}
	
	public static List<String> processJar(String inFile) throws IOException {
		List<String> classes = new ArrayList<String>();
		
		ZipInputStream inJar = null;
		try {
			try {
				inJar = new ZipInputStream(new BufferedInputStream(new FileInputStream(inFile)));
			} catch (FileNotFoundException var24) {
				throw new FileNotFoundException("Could not open input file: " + var24.getMessage());
			}

			while(true) {
				ZipEntry e = inJar.getNextEntry();
				if(e == null) {
					return classes;
				}

				if(!e.isDirectory()) {
					byte[] data = new byte[4096];
					ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();

					int len;
					do {
						len = inJar.read(data);
						if(len > 0) {
							entryBuffer.write(data, 0, len);
						}
					} while(len != -1);

					byte[] entryData = entryBuffer.toByteArray();
					String entryName = e.getName();
					if(entryName.endsWith(".class") && !entryName.startsWith(".")) {
						classes = (List<String>) CollectionUtils.union(processClass(inFile, entryName), classes);
					}
				}
			}
		} finally {
			if(inJar != null) {
				try {
					inJar.close();
				} catch (IOException var21) {
					;
				}
			}

		}
	}
}
