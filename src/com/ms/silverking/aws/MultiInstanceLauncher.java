package com.ms.silverking.aws;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.UserIdGroupPair;

public class MultiInstanceLauncher {

	private final InetAddress ip;
	private final int numInstances;
	private final AmazonEC2 ec2;
	
	private Instance launchInstance;
	private String amiId;
	private String instanceType;
	private String keyPairName;
	private List<GroupIdentifier> securityGroups;
	
	private String privateKey;
	private String privateKeyFilename;
	private String nonLaunchMachinesIpList;
	
	private static final String newKeyName = "sk_key";
	private static final String newSecurityGroupName = "sk_instance";
	
	private Reservation workerInstances;

	private static final String userHome = System.getProperty("user.home");
	private static final String newLine  = System.getProperty("line.separator");
	
	private boolean debugPrint = false;
	
	public MultiInstanceLauncher(InetAddress ip, int numInstances) {
		this.ip = ip;
		this.numInstances = numInstances;
		privateKeyFilename      = userHome + "/.ssh/id_rsa";
		nonLaunchMachinesIpList = userHome + "/SilverKing/build/aws/multi_nonlaunch_machines_list.txt";
		ec2 = AmazonEC2ClientBuilder.defaultClient();
	}
	
	public void run() {
		setLaunchInstance();
//		createSecurityGroup();
//		addSecurityGroupToLaunchInstance();
		createKeyPair();
		createPrivateKeyFile();
		runInstances();
		createIpListFile();
	}
	
	private void createSecurityGroup() {
//		DescribeSecurityGroupsResult dsgResult = ec2.describeSecurityGroups();
//		debugPrint(dsgResult);
		
		CreateSecurityGroupRequest sgRequest = new CreateSecurityGroupRequest();
		sgRequest.withGroupName(newSecurityGroupName)
				 .withDescription("For running sk instance(s)");
		CreateSecurityGroupResult createSecurityGroupResult = ec2.createSecurityGroup(sgRequest);

		IpPermission ipPermission = new IpPermission();
		UserIdGroupPair pair = new UserIdGroupPair();
		pair.withGroupName(newSecurityGroupName)	// or could have used .withGroupId(createSecurityGroupResult.getGroupId()) instead
			.withDescription("so machines can talk to each other");	
		ipPermission.withUserIdGroupPairs(Arrays.asList(pair))
		            .withIpProtocol("-1")
			        .withFromPort(-1)
			        .withToPort(-1);
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupName(newSecurityGroupName)
		                                    .withIpPermissions(ipPermission);
			
		ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
	}
	
	private void setLaunchInstance() {
		print("Setting Launch Host");
		
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		while (true) {
		    DescribeInstancesResult response = ec2.describeInstances(request);

		    for (Reservation reservation : response.getReservations()) {
		        for (Instance instance : reservation.getInstances()) {
		            printInstance(instance);
			        if ( isLaunchInstance(instance) ) {
			        	setLaunchInstance(instance);
			        	return;
			        }
		        }
		    }

		    if (response.getNextToken() == null) {
		        break;
		    }
		    
		    debugPrint("token: " + response.getNextToken());
		    request.setNextToken(response.getNextToken());
		}

		throw new RuntimeException("Couldn't find launch instance");
	}
	
	private void printInstance(Instance instance) {
		if (debugPrint)
			System.out.printf(
	                "Found instance with id %s, " +
	                "AMI %s, " +
	                "type %s, " +
	                "state %s " +
	                "and monitoring state %s%n",
	                instance.getInstanceId(),
	                instance.getImageId(),
	                instance.getInstanceType(),
	                instance.getState().getName(),
	                instance.getMonitoring().getState());
	}
	
	private boolean isLaunchInstance(Instance instance) {
		if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
			return isRunning(instance) && instance.getImageId().equals("ami-b77b06cf");
		else
			return isRunning(instance) && ipMatchesThisMachine(instance);				
	}
	
	private boolean isRunning(Instance instance) {
		return instance.getState().getName().equals("running");
	}
	
	private boolean ipMatchesThisMachine(Instance instance) {
		return instance.getPrivateIpAddress().equals(ip.getHostAddress());
	}
	
	private void setLaunchInstance(Instance instance) {
		launchInstance = instance;
    	amiId          = instance.getImageId();
    	instanceType   = instance.getInstanceType();
    	keyPairName    = instance.getKeyName();
    	securityGroups = instance.getSecurityGroups();
    	printDetails();
    	printDone(instance.getInstanceId());
	}
	
	private void printDetails() {
		debugPrint("set launch instance: " + launchInstance);
		debugPrint("ami:  " + amiId);
		debugPrint("type: " + instanceType);
		debugPrint("kp:   " + keyPairName);
		debugPrint("sg:   " + securityGroups);
	}
	
	private void createKeyPair() {
		print("Creating Key Pair");
		
		DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest();
		deleteKeyPairRequest.withKeyName(newKeyName);

		DeleteKeyPairResult deleteKeyPairResult = ec2.deleteKeyPair(deleteKeyPairRequest);
		
		CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
		createKeyPairRequest.withKeyName(newKeyName);

		CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
		
		KeyPair keyPair = createKeyPairResult.getKeyPair();
		privateKey = keyPair.getKeyMaterial();
		
		printDone(newKeyName);
	}
	
	private void createPrivateKeyFile() {
		print("Creating Private Key File");
		
		writeToFile(privateKeyFilename, privateKey);
		 
//		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
//		ProcessBuilder builder = new ProcessBuilder();
//		if (isWindows) {
//			builder.command("cmd.exe", "/c", "echo " + privateKey + " >> .ssh/id_rsa");
//		} else {
//			builder.command("sh", "-c", "echo " + privateKey + " >> .ssh/id_rsa");
//		}
//	    File f = new File(System.getProperty("user.home"));
//		builder.directory(f);
//		Process process;
//		try {
//			process = builder.start();
//			int exitCode = process.waitFor();
//			assert exitCode == 0;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		printDone(privateKeyFilename);
	}
	
	private void writeToFile(String filename, String content) {
		File file = new File(filename);
		  
		try {
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	private void runInstances() {
		print("Running Instances");
		
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withImageId(amiId)
		                   .withInstanceType(instanceType)
		                   .withMinCount(1)
		                   .withMaxCount(numInstances)
		                   .withKeyName(newKeyName)
		                   .withSecurityGroups( getNames(securityGroups) );
				
		RunInstancesResult result = ec2.runInstances(runInstancesRequest);
		workerInstances = result.getReservation();
		
		printDone( getIds(workerInstances) );
	}
	
	private List<String> getNames(List<GroupIdentifier> securityGroups) {
		List<String> names = new ArrayList<>();
		
		for (GroupIdentifier group : securityGroups) {
			names.add(group.getGroupName());
		}
		
		return names;
	}
	
	private String getIds(Reservation reservation) {
		String ids = "";
		
		for (Instance instance : reservation.getInstances()) {
			ids += instance.getInstanceId() + ", ";
		}
		
		return ids;
	}
	
	private void createIpListFile() {
		print("Creating IpList File");
		
		String ipList = "";
		for (Instance instance : workerInstances.getInstances()) {
			ipList += instance.getPrivateIpAddress() + newLine;
		}
		
		writeToFile(nonLaunchMachinesIpList, ipList);
		
		printDone(nonLaunchMachinesIpList);
	}
	
	private void print(String text) {
		System.out.printf("%-25s ... ", text);
	}
	
	private void printDone(String value) {
		System.out.println("done ("+value+")");
	}
	
	private void debugPrint(String text) {
		if (debugPrint)
			System.out.println(text);
	}
	
    public static void main(String[] args) throws Exception {
    	InetAddress ip = InetAddress.getLocalHost();
        System.out.println("ip = "+ip.getHostAddress());
        
        if (args.length == 0)
        	throw new RuntimeException("We need to know how many instances to start. Please pass in <numberOfInstances>");
        
        int numInstances = Integer.valueOf(args[0]);
        int nonLaunchInstances = numInstances - 1;
        if (nonLaunchInstances <= 0)
        	throw new RuntimeException("numberOfInstances needs to be > 1");
        
        MultiInstanceLauncher launcher = new MultiInstanceLauncher(ip, nonLaunchInstances);
        launcher.run();
	}

}
