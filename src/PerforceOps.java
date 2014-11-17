import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;


public class PerforceOps {
    
    private static String[] users;
    private static String source_branch;
    private static String target_branch;
    private static String integrationChangelistNumber;
    private static String usersString;
                
    /**
     * @param args
     * @throws InterruptedException 
     * @throws IOException 
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        source_branch = System.getenv("SOURCE_BRANCH");
        target_branch = System.getenv("TARGET_BRANCH");
        integrationChangelistNumber = System.getenv("NEW_PENDING_CHANGELIST");
        
        usersString = System.getenv("USERS").replace(",",", ");
        users = usersString.split(",");
       
        HashSet<String> fileList = getSourceFilesToIntegrate();

        System.out.println("\nFiles modified by [" + usersString + "] in " + source_branch + " :");

        for (String file: fileList) {
            System.out.println(file);
        }        
        
        System.out.println();
        
        integrateFiles(fileList);
        
        resolveFiles();
    }

    public static void resolveFiles() throws IOException {
        System.out.println("\nResolving files in changelist : " + integrationChangelistNumber);

        String command = "p4 resolve -am -c " + integrationChangelistNumber;

        System.out.println(command);

        Process p = Runtime.getRuntime().exec(command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String line = "";

        while (isRunning(p)) {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                System.out.println(line);
            }

            while ((line = errorReader.readLine()) != null) {
                System.out.println(line);
            }
        }

    }
    
    public static void integrateFiles(HashSet<String> fileList ) throws IOException {
        System.out.println("Integrating files from " + source_branch + " to " + target_branch + "\n");
        
        String command_prefix = "p4 integrate -f -c " + integrationChangelistNumber + " ";
        
        for (String file: fileList) {
            String targetFilePath = file.replace(source_branch, target_branch);
            String command = command_prefix + file + " " + targetFilePath;
            
            System.out.println(command);
            Process p = Runtime.getRuntime().exec(command);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            
            String line = "";           
            
            while (isRunning(p)) {
                while ((line = reader.readLine())!= null) {
                   line = line.trim();
                   System.out.println(line);
                }
                
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
            }  
            
            System.out.println();
        }
    }
    
    // get all the distinct source files modified by users in users[] array in branch
    public static  HashSet<String> getSourceFilesToIntegrate() throws IOException, InterruptedException {
        HashSet<String> fileList = new HashSet<String>();

        for (String user: users) {
            user = user.trim();
            System.out.println("Checking changelists by user " + user + " in " + source_branch);
            
            Process p = Runtime.getRuntime().exec("p4 changes -u " + user + " " + source_branch + "...");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line = "";           
            
            while (isRunning(p)) {
                while ((line = reader.readLine())!= null) {
                   String lineArgs[] = line.split(" ");
                   
                   if (lineArgs[0].equals("Change")) {
                       String changelistNumber = lineArgs[1];
                       System.out.println(changelistNumber);
                       HashSet<String> changelistFiles = getFilesInChangeList(changelistNumber);
                       fileList.addAll(changelistFiles);
                   }
                }
                
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            System.out.println();
        }   
        
        return fileList;
    }
    
    public static HashSet<String> getFilesInChangeList(String changelistNumber) throws IOException {
        String command = "p4 describe -s " + changelistNumber;
        
        Process p = Runtime.getRuntime().exec(command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        HashSet<String> fileList = new HashSet<String>();
        
        String line = "";           
        
        while (isRunning(p)) {
            while ((line = reader.readLine())!= null) {
               line = line.trim();
               if (line.contains(source_branch)) {
                   line = line.substring(line.indexOf(source_branch), line.indexOf("#"));
                   fileList.add(line);
               }
            }
            
            while ((line = errorReader.readLine()) != null) {
                System.out.println(line);
            }
        }  

        return fileList;
    }
    
    public static boolean isRunning(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }
}
