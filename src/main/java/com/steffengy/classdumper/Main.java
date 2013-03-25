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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import static java.util.Arrays.asList;

public class Main 
{
	private static boolean isInheritance = false;
	
	private static OptionSet options;
	public static void main(String[] args) throws IOException
	{
		OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList("?", "help"), "Show the help");

                acceptsAll(asList("i", "jarFile"), "The input jar file")
                        .withRequiredArg()
                        .ofType(String.class);

                acceptsAll(asList("o", "outputFile"), "The output, if not specified, print to console")
                        .withRequiredArg()
                        .ofType(String.class);

                acceptsAll(asList("s", "inheritance"), "Print inheritance instead of classes used");
            }
        };
        
        try {
            options = parser.parse(args);
        } catch (OptionException ex) {
            System.out.println(ex.getLocalizedMessage());
            System.exit(-1);
            return;
        }

        if (options == null || options.has("?")) {
            try {
                parser.printHelpOn(System.err);
            } catch (IOException ex) {
                System.out.println(ex.getLocalizedMessage());
            }
            System.exit(-1);
            return;
        }
        
        if(options.has("inheritance"))
        	isInheritance = true;
        
        if(options.has("jarFile"))
        {
        	List<String> classes = processJar((String) options.valueOf("jarFile"));
        	if(!options.has("outputFile"))
				for(String str : classes)
					System.out.println(str);
			else
				FileUtils.writeLines(new File((String) options.valueOf("outputFile")), classes);
			System.out.println(classes.size() + " entrys");
        }
        
        
	}
	
	private static List<String> processClass(String inFile, String entryName) throws ClassFormatException, IOException 
	{
		List<String> clses = new ArrayList<String>();
		ClassParser parser = new ClassParser(inFile, entryName);
		JavaClass javaClass = parser.parse();
		
		if(!isInheritance)
		{
			ConstantPool realConstantPool = javaClass.getConstantPool();
			Constant[] constantPool = realConstantPool.getConstantPool();
			for(Constant constant : constantPool)
			{
				if(constant instanceof ConstantClass)
				{
					ConstantClass cls = (ConstantClass) constant;
					clses.add(cls.getBytes(realConstantPool));
				}
				else if(constant instanceof ConstantUtf8)
				{
					String utf8 = ((ConstantUtf8) constant).getBytes();
					if(utf8.startsWith("L") && utf8.endsWith(";"))
					{
						clses.add(utf8.substring(1, utf8.length() - 1));
					}
				}
			}
		}
		else
		{
			String interfaces = StringUtils.join(javaClass.getInterfaceNames(), ";");
			
			String superName = "";
			try{
				for(JavaClass cl : javaClass.getSuperClasses())
				{
					superName += cl.getClassName() + ";";
				}
			} 
			catch (ClassNotFoundException e)
			{
				superName += ";";
			}
			
			clses.add((javaClass.getClassName() + "|" + superName + "|" + interfaces).replace(".", "/"));
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
