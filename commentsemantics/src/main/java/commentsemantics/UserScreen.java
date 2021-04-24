package commentsemantics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;

import featurelocation.LsiFeatureLocation;
import featurelocation.VsmDocSimilarity;
import featurelocation.VsmFeatureLocation;

import parser.FileParser;
import parser.ProjectParser;
import queryresult.ResultStore;

public class UserScreen 
{
	static JPanel contentPane = new JPanel();	
	
	static JButton button1 = new JButton("Browse Project");
	static JButton queryButton = new JButton("Run Query");
	
	static JCheckBox check_removeCodeComments = new JCheckBox("Remove Code-Comments");
	static JCheckBox check_includeArtefacts = new JCheckBox("Include Artefacts");
	
	static JFileChooser fchooser = new JFileChooser();
	
	static JLabel l3 = new JLabel("Query:");
	static JTextField text_query = new JTextField();
	
	//static JLabel lResult = new JLabel("Result:");
	//static JTextArea textResult = new JTextArea();
	
	//radio buttons
	static JLabel radio_Searchlabel = new JLabel("Search Options:");
	JRadioButton radio_allcomments=new JRadioButton("All Comments", true);    
	JRadioButton radio_linecomments=new JRadioButton("Line Comments");
	JRadioButton radio_blockcomments=new JRadioButton("Block Comments");
	JRadioButton radio_doccomments=new JRadioButton("Javadoc Comments");
	ButtonGroup bgSeachOption = new ButtonGroup();
	
	static JLabel radio_FLlabel = new JLabel("FL Techniques:");
	JCheckBox check_vsm=new JCheckBox("VSM");    
	JCheckBox check_lsi=new JCheckBox("LSI");	
	
	//non-UI elements
	private String projDir 			 = "";
		
	VsmFeatureLocation vsmFL = new VsmFeatureLocation();
	LsiFeatureLocation lsiFL = new LsiFeatureLocation();
	VsmDocSimilarity vsmDocSimilarity = new VsmDocSimilarity();
	
	FileParser fileParser = new FileParser();
	ProjectParser projParser = new ProjectParser(fileParser);
	
	ResultStore resultStore = new ResultStore();
	
	public void showUI() 
	{
		JFrame  myUI = new JFrame();
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.setBounds(0, 0, 425, 525);
		myUI.setTitle("Source code analysis project");

		contentPane.setLayout(null);
		myUI.setContentPane(contentPane);
		
		button1.setBounds(50, 40, 140, 25);
		contentPane.add(button1);
		
		l3.setBounds(50, 90, 300, 20);
		contentPane.add(l3);
		text_query.setBounds(50, 110, 300, 20);
		contentPane.add(text_query);
		
		check_removeCodeComments.setBounds(48, 130, 180, 20);
		contentPane.add(check_removeCodeComments);
		
		check_includeArtefacts.setBounds(230, 130, 200, 20);
		contentPane.add(check_includeArtefacts);
		
		//lResult.setBounds(50, 170, 100, 20);
		//contentPane.add(lResult);
		//textResult.setBounds(50, 190, 300, 100);
		//contentPane.add(textResult);
		
		
		//radio buttons
		bgSeachOption.add(radio_allcomments);
		bgSeachOption.add(radio_linecomments);
		bgSeachOption.add(radio_blockcomments);
		bgSeachOption.add(radio_doccomments);
		
		contentPane.add(radio_allcomments);
		contentPane.add(radio_linecomments);
		contentPane.add(radio_blockcomments);
		contentPane.add(radio_doccomments);
		contentPane.add(check_vsm);
		contentPane.add(check_lsi);
		contentPane.add(radio_Searchlabel);
		contentPane.add(radio_FLlabel);
		
		radio_Searchlabel.setBounds(50, 170, 150, 20);			
		radio_allcomments.setBounds(48, 190, 150, 20);
		radio_linecomments.setBounds(48, 220, 150, 20);
		radio_blockcomments.setBounds(48, 250, 150, 20);
		radio_doccomments.setBounds(48, 280, 150, 20);
		
		radio_FLlabel.setBounds(50, 320, 150, 20);			
		check_vsm.setBounds(48, 340, 150, 20);
		check_lsi.setBounds(48, 370, 150, 20);
		
		queryButton.setBounds(50, 420, 140, 25);
		contentPane.add(queryButton);
		
		//non-UI elements
		fileParser.setVsmFL(vsmFL);
		fileParser.setLsiFL(lsiFL);
		fileParser.setDocumentSimilarity(vsmDocSimilarity);
		
				
		myUI.setVisible(true);
		
		//Browse directory
		button1.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) {
				fchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fchooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
				
				int result = fchooser.showOpenDialog(myUI.getComponent(0));
				
				if (result == JFileChooser.APPROVE_OPTION) {
					projDir = fchooser.getSelectedFile().getAbsolutePath();
					
					System.out.println("Project Directory:" + projDir);					
				}							
			}
		});
		
		//run query
		queryButton.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent arg0) {
				
				if(!validateInput(true))
					return;				
				
				//user select
				String query = text_query.getText();
				
				resultStore.OpenStore(query);
				fileParser.reset();
				
				boolean removeCodeComments = check_removeCodeComments.isSelected();
				boolean includeArtefacts = check_includeArtefacts.isSelected();
				boolean allComments = radio_allcomments.isSelected();
				boolean lineComments = radio_linecomments.isSelected();
				boolean blockComments = radio_blockcomments.isSelected();
				boolean docComments = radio_doccomments.isSelected();
				
				fileParser.setRemoveCodeComments(removeCodeComments);
				fileParser.setIncludeArtefacts(includeArtefacts);
				fileParser.setUseAllComments(allComments);
				fileParser.setUseLineComments(lineComments);
				fileParser.setUseBlockComment(blockComments);
				fileParser.setUseJavadocComment(docComments);
				
				try 
				{
					projParser.parseProject(new File(projDir));	
					
					List<String> docs = null;
					
					if(check_vsm.isSelected()) {
						docs = vsmFL.VsmQuerySearch(query);
						resultStore.PersistVsmQueryResult(query, docs);
						
						//Find the similar document using the VSM cosine similarity for topdoc
						if(docs.size() > 0) {
							List<String> similarDocs = vsmDocSimilarity.vsmGetSimilarDocuments(docs.get(0));
							resultStore.PersistSimilarDocResult(docs.get(0), similarDocs);
						}
					}
					
					if(check_lsi.isSelected()) {
						docs = lsiFL.LsiQuerySearch(query);
						resultStore.PersistLsiQueryResult(query, docs);
						
						//Find the similar document using the VSM cosine similarity for topdoc
						if(docs.size() > 0) {
							List<String> similarDocs = vsmDocSimilarity.vsmGetSimilarDocuments(docs.get(0));
							resultStore.PersistSimilarDocResult(docs.get(0), similarDocs);
						}
					}
				} 
				catch (IOException e) 
				{
					e.printStackTrace();					
				}
				finally 
				{
					resultStore.CloseStore();
				}				
			}
		});
		
	}
	
	private boolean validateInput(boolean checkBox)
	{
		if (projDir.isEmpty()) {
			JOptionPane.showMessageDialog(null, "chose the project folder before run!");
			return false;
		}
		
		if (text_query.getText().isEmpty()) {
			JOptionPane.showMessageDialog(null, "Enter query string before run!");
			return false;
		}	
		
		if (!check_lsi.isSelected() && !check_vsm.isSelected()) {
			JOptionPane.showMessageDialog(null, "chose feature location technique before run!");
			return false;
		}
		
		return true;
	}

}
