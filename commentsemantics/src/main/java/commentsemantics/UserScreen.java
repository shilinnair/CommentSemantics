package commentsemantics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import parser.FileParser;

public class UserScreen 
{
	static JPanel contentPane = new JPanel();	
	
	static JButton button1 = new JButton("Browse");
	static JButton button2 = new JButton("Run Query");
	
	static JCheckBox c1 = new JCheckBox("Comments");
	static JCheckBox c2 = new JCheckBox("Artefact");
	
	static JFileChooser fchooser = new JFileChooser();
	
	static JLabel l3 = new JLabel("Query:");
	static JTextField query = new JTextField();
	
	static JLabel lResult = new JLabel("Result:");
	static JTextArea textResult = new JTextArea();
	
	static FileParser javaParser 	 = new FileParser();
	
	
	private String projDir 			 = "";
	
	List<String> queryResult = new ArrayList<String>();
	
	public void showUI() 
	{
		JFrame  myUI = new JFrame();
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.setBounds(0, 0, 425, 500);
		myUI.setTitle("Source code analysis project");

		contentPane.setLayout(null);
		myUI.setContentPane(contentPane);
		
		button1.setBounds(50, 40, 80, 30);
		contentPane.add(button1);
		
		l3.setBounds(50, 90, 300, 20);
		contentPane.add(l3);
		query.setBounds(50, 110, 300, 20);
		contentPane.add(query);
		
		c1.setBounds(48, 130, 100, 20);
		contentPane.add(c1);
		
		c2.setBounds(150, 130, 100, 20);
		contentPane.add(c2);

		button2.setBounds(250, 130, 100, 20);
		contentPane.add(button2);
		
		lResult.setBounds(50, 170, 100, 20);
		contentPane.add(lResult);
		textResult.setBounds(50, 190, 300, 100);
		contentPane.add(textResult);
				
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
					
					queryResult.clear();  // clear previous query results
					
					System.out.println("Project Directory:" + projDir);					
				}							
			}
		});
		
		//run query
		button2.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent arg0) {
				
				if(!validateInput(true))
					return;
				
				
				
				if (c2.isSelected()) { // Artifact
					//queryResult = flQuery.runQuery(query.getText(), "artefact");
				} 
				else if (c1.isSelected()) { // comments
					//queryResult = flQuery.runQuery(query.getText(), "comments");
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
		
		if (query.getText().isEmpty()) {
			JOptionPane.showMessageDialog(null, "Enter query string before run!");
			return false;
		}	
		
		if (checkBox) {
			if (!c1.isSelected() && !c2.isSelected()) {
				JOptionPane.showMessageDialog(null, "chose Artefact or Comments before run!");
				return false;
			}
		}
		
		return true;
	}

}
