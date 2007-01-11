/*
 * Created on 29/11/2005
 * justinw5
 * 
 */
package au.gov.naa.digipres.xena.viewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import au.gov.naa.digipres.xena.core.NormalisedObjectViewFactory;
import au.gov.naa.digipres.xena.core.Xena;
import au.gov.naa.digipres.xena.kernel.IconFactory;
import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.view.XenaView;
import au.gov.naa.digipres.xena.util.GlassPane;

/**
 * Starting point for Xena Viewer. Only real function is to 
 * select a xena file with the file chooser, retrieve a XenaView
 * from the NormalisedObjectViewFactory, and then display this 
 * XenaView in a NormalisedObjectViewFrame.
 * 
 * @author justinw5
 * created 30/11/2005
 * xena
 * Short desc of class:
 */
public class ViewerMainFrame extends JFrame
{
	private Xena xenaInterface;
	private Preferences prefs;
	
	private static final String XENADEST_DIR_KEY = "dir/xenadest";
	private static final String LAST_DIR_VISITED_KEY = "dir/lastvisited";
	
	public ViewerMainFrame()
	{
		super("Xena Viewer");
		initPrefs();
		initGUI();
	}
	
	public ViewerMainFrame(File xenaFile)
	{
		this();
		try
		{
			displayView(xenaFile);
		}
		catch (Exception e)
		{
			handleXenaException(e);
		}
	}
	
	public ViewerMainFrame(Exception startupException)
	{
		this();
		handleXenaException(startupException);
	}
	
	private void initPrefs()
	{
		prefs = Preferences.userNodeForPackage(ViewerMainFrame.class);
	}

	/**
	 * One-time initialisation of GUI components
	 */
	private void initGUI()
	{
		this.setLocation(120, 120);
		this.setResizable(false);
		this.setIconImage(IconFactory.getIconByName("images/xena-splash.png").getImage());
		
		// Setup Menu
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
//		JMenu editMenu = new JMenu("Edit");
		
		JMenuItem openItem = new JMenuItem("Open");
//		JMenuItem prefsItem = new JMenuItem("Preferences");
		JMenuItem exitItem = new JMenuItem("Exit");
		
		fileMenu.add(openItem);
		fileMenu.add(exitItem);
//		editMenu.add(prefsItem);
		
		menuBar.add(fileMenu);
//		menuBar.add(editMenu);
		
		this.setJMenuBar(menuBar);
		
		
		// Setup toolbar
		JPanel toolbarPanel = new JPanel(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		
		JButton openButton = new JButton("Open");
		toolBar.add(openButton);
		toolbarPanel.add(toolBar, BorderLayout.NORTH);
		
		// Needs to be uncommented if using Metal L&F
//		toolbarPanel.setBorder(new EtchedBorder());
		
		/*
		 * Main window area - currently not in use
		 * as opened Xena files will be popup windows.
		 */ 
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new EtchedBorder());
		
		
		// Layout panels
		this.getContentPane().add(toolbarPanel, BorderLayout.NORTH);
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		
		// Handle window close
		this.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e)
			{
				doShutdown();
			}
						
		});
		
		// Handle open from menu
		openItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				try
				{
					openXenaFile();
				}
				catch (XenaException e1)
				{
					handleXenaException(e1);				
				}
			}
			
		});

		// Handle open from toolbar
		openButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				try
				{
					openXenaFile();
				}
				catch (XenaException e1)
				{
					handleXenaException(e1);
				}
			}
			
		});
		
//		// Handle Preferences menu action
//		prefsItem.addActionListener(new ActionListener(){
//
//			public void actionPerformed(ActionEvent e)
//			{
//				showPreferencesDialog();
//			}
//			
//		});
		
		// Handle exit menu action
		exitItem.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e)
			{
				doShutdown();
			}
			
		});
		
	}
	
//	/**
//	 * Show "Edit Preferences" dialog. Existing preferences will be
//	 * loaded using Java preferences, and saved after the dialog has
//	 * been (successfully) closed.
//	 *
//	 */
//	private void showPreferencesDialog()
//	{
//		ViewerPreferencesDialog prefsDialog = new ViewerPreferencesDialog(this);
//		prefsDialog.setXenaDestDir(prefs.get(XENADEST_DIR_KEY, ""));
//		prefsDialog.setLocation(this.getX()+25, this.getY()+25);
//		prefsDialog.setVisible(true);
//		
//		// We have returned from the dialog
//		if (prefsDialog.isApproved())
//		{
//			prefs.put(XENADEST_DIR_KEY, prefsDialog.getXenaDestDir());
//		}
//	}

	/**
	 * The user selects a xena file using the FileChooser.
	 * The XenaView representing this file is then retrieved
	 * from NormalisedObjectViewFactory (the default view type
	 * will be used). The XenaView is then passed to the displayView
	 * method.
	 * 
	 * @throws XenaException
	 * @throws IOException
	 */
	private void openXenaFile() throws XenaException
	{
		// Setup file chooser dialog		
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new XenaFileFilter());
		
		/*
		 * Get the default directory which the file chooser will initially
		 * display. The Last Directory Visited preference is the first option.
		 * If that has not been set, then the Xena Destination Directory will be
		 * used. If that has not been set, then the file chooser default will
		 * be used.
		 */
		
		String defaultDir = prefs.get(LAST_DIR_VISITED_KEY,
		                              prefs.get(XENADEST_DIR_KEY, null));
		
		if (defaultDir != null)
		{
			fileChooser.setCurrentDirectory(new File(defaultDir));
		}
		
		// Open file chooser
		int retVal = fileChooser.showOpenDialog(this);
		
		// Have returned fron dialog
		if (retVal == JFileChooser.APPROVE_OPTION)
		{
			// File chosen - now to load it using Xena
			try
			{
				File xenaFile = fileChooser.getSelectedFile();
				displayView(xenaFile);
				
				// Save last file chooser directory to prefs
				prefs.put(LAST_DIR_VISITED_KEY, 
				          fileChooser.getCurrentDirectory().getPath());
			}
			catch (IOException e)
			{
				throw new XenaException(e);
			}
		}
	}
	
	/**
	 * Display the given XenaView in a NormalisedObjectViewFrame
	 * @param xenaView
	 * @param xenaFile
	 * @throws IOException 
	 * @throws XenaException 
	 */
	public void displayView(File xenaFile) throws XenaException, IOException
	{
		Xena xena = getXenaInterface();
		NormalisedObjectViewFactory novFactory =
			new NormalisedObjectViewFactory(xena);
		XenaView objView = novFactory.getView(xenaFile);

		NormalisedObjectViewFrame objFrame = 
			new NormalisedObjectViewFrame(objView, xenaInterface, xenaFile);
		objFrame.setLocation(this.getX()+50, this.getY()+50);
		objFrame.requestFocus();
		objFrame.setVisible(true);
	}

	private void doShutdown()
	{
		System.exit(0);
	}
	
	public void handleXenaException(Exception ex)
	{		
		ex.printStackTrace();
		JOptionPane.showMessageDialog(this, 
		                              ex.getMessage(),
		                              "Xena",
		                              JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Initialises the Xena interface (currently loads plugins) if required
	 * @return
	 * @throws XenaException
	 * @throws IOException
	 */
	private Xena getXenaInterface() throws XenaException, IOException
	{
		if (xenaInterface == null)
		{
			xenaInterface = new Xena();
			xenaInterface.loadPlugins(getPluginsDirectory());
		}
		
		return xenaInterface;
	}

	/**
	 * Returns the xena lite plugins directory. This is set as being a directory named "plugins"
	 * which is a subdirectory of the directory containing the xena.jar file.
	 * First we assume that we are running xena lite from the directory containing the xena.jar file.
	 * If the plugins directory cannot be found, then the base directory could be different to the
	 * xena.jar directory. So first we get the URL of the litegui package. This URL
	 * will consist of the file system path to the jar file plus a path to the package directory. The
	 * directory containing the jar file can thus be extracted.
	 * @return
	 * @throws XenaException
	 */
	private File getPluginsDirectory() throws XenaException
	{
		File pluginsDir = new File("plugins");
		if (!pluginsDir.exists() || !pluginsDir.isDirectory())
		{
			boolean pluginsDirFound = false;
			String resourcePath = 
				this.getClass().getResource("/" + this.getClass().getPackage().getName().replace(".", "/")).getPath();
			
			String fileIdStr = "file:";
			
			if (resourcePath.indexOf(fileIdStr) >= 0 && resourcePath.lastIndexOf("!") >= 0)
			{
				String jarPath = resourcePath.substring(resourcePath.indexOf(fileIdStr)+fileIdStr.length(), resourcePath.lastIndexOf("!"));
				if (jarPath.lastIndexOf("/") >= 0)
				{
					pluginsDir = new File(jarPath.substring(0, jarPath.lastIndexOf("/")+1) + "plugins");
					if (pluginsDir.exists() && pluginsDir.isDirectory())
					{
						pluginsDirFound = true;
						
					}
				}
			}
			if (!pluginsDirFound)
			{
				throw new XenaException("Cannot find default plugins directory. " +
	            						"Try running Xena Lite from the same directory as xena.jar.");
			}
		}			
			
		File[] pluginFiles = pluginsDir.listFiles();
		boolean foundPlugin = false;
		for (int i = 0; i < pluginFiles.length; i++)
		{
			if (pluginFiles[i].getName().endsWith(".jar"))
			{
				foundPlugin = true;
				break;
			}
		}
		if (!foundPlugin)
		{
			JOptionPane.showMessageDialog(this,
			                              "No plugins found in plugin directory " + pluginsDir.getAbsolutePath(),
			                              "No Plugins Found",
			                              JOptionPane.WARNING_MESSAGE);
		}
		return pluginsDir;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Set look and feel to the look and feel of the current OS
		try
		{
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		ViewerMainFrame mf = new ViewerMainFrame();
		mf.setSize(400, 300);
		mf.setVisible(true);
		
		if (args.length > 0)
		{
			File xenaFile = new File(args[0]);
			if (xenaFile.exists() && xenaFile.isFile())
			{
				try
				{
					GlassPane gp = GlassPane.mount(mf, true);
					gp.setVisible(true);
					mf.displayView(xenaFile);
					gp.setVisible(false);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				mf.handleXenaException(new IOException("Invalid file - " + xenaFile.getAbsolutePath()));
			}
		}
		
	}

}
