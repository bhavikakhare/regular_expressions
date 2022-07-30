package one;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//import com.github.vertical_blank.sqlformatter.SqlFormatter;

// in this class we use 7 regular expressions in total

public class regular_expressions {

	public static void main( String[] arguments ) {
		
		// VERY USEFUL WEBSITE to play around with regex
		// https://www.regexplanet.com/advanced/java/index.html
		
//		List<String> queries = findAllQueries() ;
//		HashMap<String,String> map = mapQueriesToTemplates() ;
//		List<String> list = unusedQueries( map , queries ) ;
//		findClassMembers();
//		write_parameters_to_excel_sheet(parameters) ;

		Map<String,String> SQLqueries = new HashMap<String,String>() ;
		Map<String,List<String>> parameters = new HashMap<String,List<String>>() ;
		get_query_and_parameters(SQLqueries,parameters) ;
		Map<String,String> refactor = queries2refactor() ;
		Map<String,String> handle = queries_to_new_handle() ;
		for( String query : refactor.keySet() ) {
			if( handle.get(query)==null ) continue ;
			code(query,refactor.get(query),handle.get(query),SQLqueries.get(query),parameters.get(query)) ;
		}

	}
	
	public static void code( String query , String new_name , String handle , String SQLq , List<String> parameters ) {
		if( query==null || new_name==null || handle==null || SQLq==null || parameters==null ) {
			System.out.println("query ("+query+") renamed ("+new_name+") of handle("+handle+") and SQL("+SQLq+") has parameters("+")") ;
			return ;
		}
		Path dbService_folder = Paths.get("") ;
		Path controller = Paths.get(dbService_folder+"\\controller\\DBServiceController.java") ;
		Path service = Paths.get(dbService_folder+"\\services\\"+handle+"Service.java") ;
		Path repository = Paths.get(dbService_folder+"\\repositories\\customrepo\\"+handle+"CustomRepository.java") ;
		Path SQLfile = Paths.get(dbService_folder+"\\beans\\SQLProperties.java") ;
		String paramsget = "" , signatureparams = "" , sendparams = "" , setparams = "" , notnullparams = "" ;
		for( int i=0 ; i<parameters.size() ; i++ ) {
			String p = parameters.get(i) ;
			if( i!=0 ) {
				paramsget = paramsget + ", " ;
				signatureparams = signatureparams + ", " ;
				sendparams = sendparams + ", " ;
				notnullparams = notnullparams + " || " ;
			}
			paramsget = paramsget + "params.get(\""+p+"\")" ;
			signatureparams = signatureparams + "String "+p ;
			sendparams = sendparams + p ;
			notnullparams = notnullparams + p+"!=null" ;
			setparams = setparams + "\r\n	query.setParameter(\""+p+"\", "+p+");" ;
		}
		String sqlproperty = "\nprivate String "+query+";";
		String endpoint = "\r\n" + 
				"	@GetMapping(path = \"/"+new_name+"\")\r\n" + 
				"	public ResponseEntity<List<Map<String, Object>>> "+new_name+"(\r\n" + 
				"			@RequestParam Map<String, String> params) {\r\n" + 
				"\r\n" + 
				"		List<Map<String, Object>> listRow = "+Character.toLowerCase(handle.charAt(0))+handle.substring(1)+"Service."+new_name+"("+paramsget+");\r\n" + 
				"		return ResponseEntity.ok(listRow);\r\n" + 
				"	}\r\n" + 
				"" ;
		String serviceFunction = "\r\n" + 
				"	public List<Map<String, Object>> "+new_name+"("+signatureparams+") {\r\n" + 
				"\r\n" + 
				"		List<Map<String, Object>> listRows = null;\r\n" + 
				"		if ("+notnullparams+")\r\n" + 
				"			listRows = "+Character.toLowerCase(handle.charAt(0))+handle.substring(1)+"CustomRepository."+new_name+"("+sendparams+");\r\n" + 
				"		else\r\n" + 
				"			throw new BadParametersException(\"Input parameters are not correct\");\r\n" + 
				"\r\n" + 
				"		return listRows;\r\n" + 
				"	}" ;
		String repositoryFunction = "\r\n" + 
				"	@SuppressWarnings(\"unchecked\")\r\n" + 
				"	public List<Map<String, Object>> "+new_name+"("+signatureparams+") {\r\n" + 
				"\r\n" + 
				"		Query query = entityManager.createNativeQuery(sqlProperties.get"+Character.toUpperCase(query.charAt(0))+query.substring(1)+"());\r\n" + 
				setparams + 
				"\r\n" + 
				"		NativeQueryImpl<Map<String, Object>> nativeQuery = (NativeQueryImpl<Map<String, Object>>) query;\r\n" + 
				"		nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);\r\n" + 
				"		List<Map<String, Object>> result = nativeQuery.getResultList();\r\n" + 
				"\r\n" + 
				"		return result;\r\n" + 
				"		\r\n" + 
				"	}" ;
		try {
		Files.write( SQLfile , sqlproperty.getBytes() , StandardOpenOption.APPEND ) ; 
		Files.write( controller , endpoint.getBytes() , StandardOpenOption.APPEND ) ; 
		Files.write( service , serviceFunction.getBytes() , StandardOpenOption.APPEND ) ; 
		Files.write( repository , repositoryFunction.getBytes() , StandardOpenOption.APPEND ) ; 
		} catch ( Exception e ) { e.printStackTrace(); }
		
	}
	
	public static List<String> write_parameters_to_excel_sheet( Map<String,List<String>> parameters ) {
		List<String> ALLparameters = new ArrayList<String>() ;
		try {
			File file = new File("") ;
			FileInputStream fis=new FileInputStream(file) ;   
			XSSFWorkbook wb=new XSSFWorkbook(fis) ;   
			XSSFSheet sheet_parameters_map=wb.getSheetAt(4) , sheet_all_parameters=wb.getSheetAt(5) ;
			int i = 0 , j = 0 ;
			for( String s : parameters.keySet() ) {
				Row row = sheet_parameters_map.createRow(i) ;
				int cellnumber = 0 ;
				row.createCell(cellnumber).setCellValue(s) ;
				for( String p : parameters.get(s) ) {
					Cell pc = row.createCell(++cellnumber) ;
					pc.setCellValue(p) ;
					System.out.println(pc.getStringCellValue()) ;
					if( !ALLparameters.contains(p) )
						ALLparameters.add(p) ;
				}
				i++ ;
			}
			for( String s : ALLparameters ) {
				sheet_all_parameters.createRow(j++).createCell(0).setCellValue(s) ;
			}
			FileOutputStream fos = new FileOutputStream(file) ;
			wb.write(fos) ;
			fos.close() ;
			wb.close() ;
			} catch ( Exception e ) { 
				e.printStackTrace() ;
				System.out.println(" \n & couldn't open excel sheet") ; 
			}
		return ALLparameters ;
	}
	
	public static Map<String,String> queries_to_new_handle() {
		Map<String,String> m = new HashMap<String,String>() , t_m = new HashMap<String,String>() ;
		try {
		FileInputStream fis=new FileInputStream(new File("")) ;   
		XSSFWorkbook wb=new XSSFWorkbook(fis) ;   
		XSSFSheet template_map=wb.getSheetAt(2) ;
		XSSFSheet handle_map=wb.getSheetAt(1) ;
		for( Row row : handle_map ) {
			if( row.getRowNum()!=0 && row.getCell(0)!=null && row.getCell(5)!=null )
				t_m.put( row.getCell(0).toString() , row.getCell(5).toString() ) ;
		}
		for( Row row : template_map ) {
			if( row.getRowNum()!=0 && row.getCell(0)!=null && row.getCell(1)!=null )
				if( t_m.containsKey(row.getCell(0).toString()) )
					m.put( row.getCell(1).toString() , t_m.get( row.getCell(0).toString() ) ) ;
				else {
//					System.out.println("template "+row.getCell(0).toString()+" not found in template-to-handle-map") ;
				}
		}
		wb.close() ;
		} catch ( Exception e ) { System.out.println(e+" \n & couldn't open excel sheet") ; }
		return m ;
	}
	
	public static Map<String,String> queries2refactor() {
		Map<String,String> m = new HashMap<String,String>() ;
		try {
		FileInputStream fis=new FileInputStream(new File("")) ;   
		XSSFWorkbook wb=new XSSFWorkbook(fis) ;   
		XSSFSheet sheet=wb.getSheetAt(3) ;
		for( Row row : sheet ) {
			if( row.getRowNum()!=0 && row.getCell(0)!=null && row.getCell(1)!=null ) {
				String s = row.getCell(1).toString() ;
				s = Character.toLowerCase(s.charAt(0))+s.substring(1) ;
				m.put( row.getCell(0).toString() , s ) ;
			}
		}
		wb.close() ;
		} catch ( Exception e ) { System.out.println(e+" \n & couldn't open excel sheet") ; }
		return m ;
	}
	
	public static void get_query_and_parameters( Map<String,String> query , Map<String,List<String>> parameters ) {

		Pattern queryPattern = Pattern.compile( "sql\\.queries\\.(\\w*)=([^\\n\\r]*)" ) ;
		Pattern parameterPattern = Pattern.compile( "(\\w*) ?= ?[?]" ) ;
		try  
		{  
			File file = new File("") ; 
			FileReader fr = new FileReader(file) ;   
			BufferedReader br = new BufferedReader(fr) ; 
//			String note_path = ".....\\resources\\prettySQL.txt" ;
//			BufferedWriter out = new BufferedWriter( new FileWriter( note_path , true ) ) ;
//			out.write("NOTE\n\n") ;
			String line;  
			while((line=br.readLine())!=null)  
			{  
			    Matcher m = queryPattern.matcher(line) ; 
			    if( m.matches() ) {
			    	String name = m.group(1) ;
			    	String query_string = m.group(2) ;
				    query.put( name , query_string ) ;
				    Matcher m2 = parameterPattern.matcher(query_string) ;
				    List<String> query_parameters = new ArrayList<String>() ;
				    while( m2.find() ) {
				    	String p = m2.group(1) ;
				    	if( !query_parameters.contains(p) )
				    		query_parameters.add(p) ;
				    }
				    parameters.put(name,query_parameters) ;
			    } else { 
			    	if( line.length()>0 ) 
			    		System.out.println("couldn't parse a query name & SQL string from : \t"+line); 
			    }
			}  
			fr.close() ; 
//			out.close() ;
//			System.out.println("\n"+query.size());
		}  
		catch( IOException e ) { e.printStackTrace(); }  
		
	}
	
	public static List<ClassMember> findClassMembers() {
		
		String folderPath = "";
		String note_path = "" ;
	    List<ClassMember> members = new ArrayList<ClassMember>() ;
		try{
			BufferedWriter out = new BufferedWriter( new FileWriter( note_path , true ) ) ;
			out.write("NOTE\n\n") ;
		
			String[] filePaths = new String[] {
					"RFADetailsHandler.java",
			};
			String content ;
	//	    HashMap<String,String> template_used_by_query = new HashMap<String,String>() ;
	//	    HashMap<String,String> file_using_query = new HashMap<String,String>() ;
			Pattern member_name = Pattern.compile("([\\w]+)[\\s]*[;=]") ;
			for( String filePath : filePaths ) {
				try {
					content = new String(Files.readAllBytes(Paths.get(folderPath+"\\"+filePath))) ;
					int mainStartsAt = content.indexOf('{') ;
					int mainEndsAt = content.lastIndexOf('}') ;
					content = content.substring(mainStartsAt+1,mainEndsAt) ;
					boolean isData = true , bracesFound = false ;
					int begin=0 , open_braces=0 , open_curly_braces=0 ;
					char c ;
				    for( int p=0 ; p<content.length() ; p++ ) {
				    	c = content.charAt(p) ;
//				    	if( c=='/' && p>0 & content.charAt(p-1)=='/' ) {
////				    		inComment1 = true ;
//				    		continue ;
//				    	}
				    	if( c=='{' ) open_curly_braces++ ;
				    	else if( c=='}' ) open_curly_braces-- ;
				    	else if( c=='(' ) open_braces++ ;
				    	else if( c==')' ) open_braces-- ;
				    	if( isData!=true && ( c=='(' || c==')' || c=='{' || c=='}' ) ) bracesFound = true ;
				    	if( open_braces==0 && open_curly_braces==0 && bracesFound==false && c=='=' ) isData = true ;
				    	if( open_braces==0 && open_curly_braces==0 && c==';' && bracesFound==false ) {
				    		isData = false ;
				    		bracesFound = false ;
				    		ClassMember cm = new ClassMember() ;
//				    		cm.memberName = member_name.matcher(content.substring(begin,p+1)).group(1) ;
				    		System.out.println("found member :\t"+cm.memberName) ;
				    		cm.fileName = filePath ;
				    		cm.startsAt = begin ;
				    		cm.endsAt = p ;
				    		out.write("\n---data-member--\n") ;
				    		out.write(content.substring(cm.startsAt,cm.endsAt+1)) ;
				    		members.add(cm) ;
				    		begin = p+1 ;
				    		continue;
				    	}
				    	String s = "a";
				    	if( (c==';'&&(content.charAt(p-1)==')') || c=='}' ) && open_braces==0 && open_curly_braces==0 && bracesFound==true ) {
//				    		if( c==')' && content.charAt(p+1)==';' )
				    		isData = false ;
				    		bracesFound = false ;
				    		ClassMember cm = new ClassMember() ;
//				    		cm.memberName = member_name.matcher(content.substring(begin,p+1)).group(1) ;
				    		System.out.println("found member :\t"+cm.memberName) ;
				    		cm.isFunction = true ;
				    		cm.fileName = filePath ;
				    		cm.startsAt = begin ;
				    		cm.endsAt = p ;
				    		out.write("\n---member-function---\n") ;
				    		out.write(content.substring(cm.startsAt,cm.endsAt+1)) ;
				    		members.add(cm) ;
				    		begin = p+1 ;
				    		continue;
				    	}
				    }
				} catch ( IOException e ) {
		            e.printStackTrace() ;
		        }
			}
		

			out.close() ;
		} catch ( Exception e ) { System.out.println(e) ; }
		return members ;
		
	}
	
	public static HashMap<String,String> mapQueriesToTemplates() {
		
		String folderPath = "";
		String[] filePaths = new String[] {
				"RFADetailsHandler.java",
				"GS_DAOHandler.java",
				"QP_DataHandler.java"
		};
		String content ;
	    HashMap<String,String> template_used_by_query = new HashMap<String,String>() ;
		Pattern pattern_SQLquery_and_JDBCtemplate = Pattern.compile("sql\\.queries\\.(\\w*)[\\s\\S]*?(jdbc\\w+)") ;
	    Pattern check_snowflake = Pattern.compile( "SnowFlake" ) ;
	    HashMap<String,String> file_using_query = new HashMap<String,String>() ;
		for( String filePath : filePaths ) {
			try {
				content = new String(Files.readAllBytes(Paths.get(folderPath+"\\"+filePath))) ;
				int mainStartsAt = content.indexOf('{') ;
				int mainEndsAt = content.lastIndexOf('}') ;
				content = content.substring(mainStartsAt+1,mainEndsAt) ;
			    Matcher m = pattern_SQLquery_and_JDBCtemplate.matcher( content ) ; 
			    while( m.find() ) {
			        String query = m.group(1) , template = m.group(2) ;
			        if( !template_used_by_query.containsKey(query) ||  check_snowflake.matcher(template_used_by_query.get(query)).find() ) {
				    	file_using_query.put(query,filePath.substring(filePath.length()-5)) ;
//			        	System.out.println( " "+camel_to_snake(query)+"\t"+template+" : "+filePath ) ;
				        template_used_by_query.put(query,template) ;
			        }
			    }
			} catch ( IOException e ) {
	            e.printStackTrace() ;
	        }
		}
		return template_used_by_query ;
		
	}
	
	public static List<String> findAllQueries() {

        List<String> matches = new ArrayList<String>() ;
		try {
			String content = new String(Files.readAllBytes(Paths.get( "" ))) ;
		    Pattern queryPattern = Pattern.compile( "sql\\.queries\\.(\\w*)" ) ;
		    Matcher m = queryPattern.matcher(content) ; 
		    while( m.find() ) {
			    matches.add( m.group(1) ) ;
//		        System.out.println( m.group(1) ) ;
		    }
		} catch ( IOException e ) {
            e.printStackTrace() ;
        }
		return matches ;
		
	}
	
	public static List<String> unusedQueries( HashMap<String,String> usedQueries , List<String> allQueries ) {
		List<String> unusedQueries = new ArrayList<String>() ;
		for( String q : allQueries ) {
			if( !usedQueries.containsKey(q) ) {
				unusedQueries.add(q) ;
//				System.out.println("\t"+q) ;
			}
		}
		return unusedQueries ;
	}
	
	public static String camel_to_snake( String s )
    {
		
        // RegEx & replacement formula as copied from G4G 
//        String regex = "([a-z])([A-Z]+)" ;
//		  String replacement = "$1_$2" ;
//        s = s
//                .replaceAll(
//                    regex , replacement )
//                .toLowerCase() ;
//		leads to strings like this : limits_balances_rppbalance & not limits_balances_RPP_balance
		
		// RegEx in JAVA can not help us here if we want to make lowercase only those substrings that are [A-Z][a-z]+
		// JAVA lets us do string.lowercase() but that will make OMGGoAway -> omg_go_away and not OMG_go_away
		
		String regex1 = "([A-Z][a-z]+)" , regex2 = "([a-z])([A-Z]+)" ;
        String replacement1 = "_$1" , replacement2 = "$1_$2" ;
        s = s
    		.replaceAll( regex1 , replacement1 )
    		.replaceAll( regex2 , replacement2 ) ;
        for( int i=0 ; i<s.length()-1 ; i++ )
        	if( s.charAt(i)>='A' && s.charAt(i)<='Z' && s.charAt(i+1)>='a' && s.charAt(i+1)<='z' )
        		s = s.substring(0,i) + (char)( s.charAt(i)-'A'+'a' ) + s.substring(i+1) ;
        return s;
        
    }
	
	public static Map<String,String> prettifySQL() {
		
		Map<String,String> matches = new HashMap<String,String>() ;
		try  
		{  
			File file = new File("") ; 
			FileReader fr = new FileReader(file) ;   
			BufferedReader br = new BufferedReader(fr) ; 
			String note_path = "" ;
			BufferedWriter out = new BufferedWriter( new FileWriter( note_path , true ) ) ;
			out.write("NOTE\n\n") ;
			String line;  
			while((line=br.readLine())!=null)  
			{  
				Pattern queryPattern = Pattern.compile( "sql\\.queries\\.(\\w*)=([^\\n\\r]*)" ) ;
			    Matcher m = queryPattern.matcher(line) ; 
			    if( m.matches() ) {
			    	String name = m.group(1) ;
			    	String query = m.group(2) ;
				    matches.put( name , query ) ;
//				    out.write(""+name+"\n\n"+SqlFormatter.format(query)+"\n\n");
			    } else { System.out.println(line); }
			}  
			fr.close() ; 
			out.close() ;
			System.out.println("\n"+matches.size());
		}  
		catch( IOException e ) { e.printStackTrace(); }  
		return matches ;
		
	}


}

// to find the contents of a java function in a java code text file 
// we must find where an open bracket closes
// in .NET we have balancing groups to help with that
// https://stackoverflow.com/questions/17003799/what-are-regular-expression-balancing-groups/17004406#17004406
// I don't know if we have them in JAVA

// I spent a long time trying to do this ^ but for it we need to be able to match each { with its }
// This is where the theory of computation we studied comes in 
// JAVA is not a regular language
// And so... regular expressions can not parse it !
// Interesting article : https://kore-nordmann.de/blog/do_NOT_parse_using_regexp.html

class ClassMember {
	boolean isFunction = false ;
	String memberName = "" ;
	String fileName = "" ;
	int startsAt = -1 ;
	int endsAt = -1 ;
	boolean toDelete = false ;
};
