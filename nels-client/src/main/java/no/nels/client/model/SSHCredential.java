package no.nels.client.model;

public class SSHCredential {
	private String host ="";
	private String username="";
	private String sshKey ="";
	public SSHCredential(String host,String username, String sshKey){
		this.host = host;
		this.username = username;
		this.sshKey = sshKey;
	}
	
	public String getHost() {return host;}
	public String getUsername() {return username;}
	public String getSshKey() {return sshKey;}
}
