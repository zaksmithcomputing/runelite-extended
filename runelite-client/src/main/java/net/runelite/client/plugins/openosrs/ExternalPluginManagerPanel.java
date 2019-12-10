package net.runelite.client.plugins.openosrs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ExternalPluginChanged;
import net.runelite.client.plugins.ExternalPluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateManager;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.VerifyException;

@Slf4j
@Singleton
class ExternalPluginManagerPanel extends PluginPanel
{
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;
	private static final ImageIcon DELETE_ICON;
	private static final ImageIcon DELETE_HOVER_ICON;
	private static final ImageIcon DELETE_ICON_GRAY;
	private static final ImageIcon DELETE_HOVER_ICON_GRAY;

	static
	{
		final BufferedImage addIcon =
			ImageUtil.recolorImage(
				ImageUtil.getResourceStreamFromClass(OpenOSRSPlugin.class, "add_icon.png"), ColorScheme.BRAND_BLUE
			);
		ADD_ICON = new ImageIcon(addIcon);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));

		final BufferedImage deleteImg =
			ImageUtil.recolorImage(
				ImageUtil.resizeCanvas(
					ImageUtil.getResourceStreamFromClass(OpenOSRSPlugin.class, "delete_icon.png"), 14, 14
				), ColorScheme.BRAND_BLUE
			);
		DELETE_ICON = new ImageIcon(deleteImg);
		DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, 0.53f));

		DELETE_ICON_GRAY = new ImageIcon(ImageUtil.grayscaleImage(deleteImg));
		DELETE_HOVER_ICON_GRAY = new ImageIcon(ImageUtil.alphaOffset(ImageUtil.grayscaleImage(deleteImg), 0.53f));
	}

	private final ExternalPluginManager externalPluginManager;
	private final UpdateManager updateManager;
	private final Font normalFont = FontManager.getRunescapeFont();
	private final Font smallFont = FontManager.getRunescapeSmallFont();

	@Inject
	private ExternalPluginManagerPanel(ExternalPluginManager externalPluginManager, EventBus eventBus)
	{
		this.externalPluginManager = externalPluginManager;
		this.updateManager = externalPluginManager.getUpdateManager();

		eventBus.subscribe(ExternalPluginChanged.class, this, this::onExternalPluginChanged);

		buildPanel();
	}

	private void onExternalPluginChanged(ExternalPluginChanged externalPluginChanged)
	{
		try
		{
			SwingUtil.syncExec(this::buildPanel);
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public static <T> Predicate<T> not(Predicate<T> t)
	{
		return t.negate();
	}

	private void buildPanel()
	{
		removeAll();

		setLayout(new BorderLayout(0, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		JLabel title = new JLabel();
		JLabel titleTest = new JLabel();
		JLabel addRepo = new JLabel(ADD_ICON);

		title.setText("External Plugin Manager");
		title.setForeground(Color.WHITE);

		titleTest.setText("rr");
		titleTest.setForeground(Color.WHITE);

		addRepo.setToolTipText("Add new repository");
		addRepo.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				JTextField owner = new JTextField();
				JTextField name = new JTextField();
				Object[] message = {
					"Repository owner:", owner,
					"Repository name:", name
				};

				int option = JOptionPane.showConfirmDialog(null, message, "Add repository", JOptionPane.OK_CANCEL_OPTION);
				if (option != JOptionPane.OK_OPTION || owner.getText().equals("") || name.getText().equals(""))
				{
					return;
				}

				if (!ExternalPluginManager.testRepository(owner.getText(), name.getText()))
				{
					JOptionPane.showMessageDialog(null, "This doesn't appear to be a valid repository.", "Error!", JOptionPane.ERROR_MESSAGE);
					return;
				}

				externalPluginManager.addRepository(owner.getText(), name.getText());
				buildPanel();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				addRepo.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				addRepo.setIcon(ADD_ICON);
			}
		});

		JPanel titleActions = new JPanel(new BorderLayout(3, 0));
		titleActions.setBorder(new EmptyBorder(0, 0, 0, 0));

		titleActions.add(addRepo, BorderLayout.WEST);
		titleActions.add(addRepo, BorderLayout.EAST);

		northPanel.add(title, BorderLayout.WEST);
		northPanel.add(titleActions, BorderLayout.EAST);

		add(northPanel, BorderLayout.NORTH);
		add(getAllPlugins(), BorderLayout.CENTER);

		revalidate();
		repaint();
	}

	private JPanel getAllPlugins()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 10));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		List<PluginInfo> availablePlugins = updateManager.getAvailablePlugins();
		List<PluginInfo> plugins = updateManager.getPlugins();
		List<String> disabledPlugins = externalPluginManager.getDisabledPlugins();

		List<PluginInfo> installedPluginsList = new ArrayList<>();
		List<PluginInfo> availablePluginsList = new ArrayList<>();

		for (PluginInfo pluginInfo : plugins)
		{
			if (availablePlugins.contains(pluginInfo) || disabledPlugins.contains(pluginInfo.id))
			{
				availablePluginsList.add(pluginInfo);
			}
			else
			{
				installedPluginsList.add(pluginInfo);
			}
		}

		panel.add(repositories(), BorderLayout.NORTH);
		panel.add(installedPlugins(installedPluginsList), BorderLayout.CENTER);
		panel.add(availablePlugins(availablePluginsList), BorderLayout.SOUTH);

		return panel;
	}

	private JLabel titleLabel(String text)
	{
		JLabel title = new JShadowedLabel();

		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(Color.WHITE);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setText("<html><body style = 'text-align:center'>" + text + "</body></html>");

		return title;
	}

	private JPanel repositories()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 5));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel repositoryPanel = new JPanel();

		repositoryPanel.setLayout(new DynamicGridLayout(0, 1, 0, 6));

		for (UpdateRepository repository : externalPluginManager.getRepositories())
		{
			repositoryPanel.add(repository(repository.getId(), repository.getUrl().toString()));
		}

		panel.add(titleLabel("Repositories"), BorderLayout.NORTH);
		panel.add(repositoryPanel, BorderLayout.SOUTH);

		return panel;
	}

	private JPanel installedPlugins(List<PluginInfo> pluginInfoList)
	{
		Set<String> deps = externalPluginManager.getDependencies();

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 5));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel pluginPanel = new JPanel();

		pluginPanel.setLayout(new DynamicGridLayout(0, 1, 0, 6));

		for (PluginInfo pluginInfo : pluginInfoList)
		{
			pluginPanel.add(pluginPanel(pluginInfo, true, deps.contains(pluginInfo.id)));
		}

		panel.add(titleLabel("Installed plugins"), BorderLayout.NORTH);
		panel.add(pluginPanel, BorderLayout.SOUTH);

		return panel;
	}

	private JPanel availablePlugins(List<PluginInfo> pluginInfoList)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 5));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel pluginPanel = new JPanel();

		pluginPanel.setLayout(new DynamicGridLayout(0, 1, 0, 6));

		for (PluginInfo pluginInfo : pluginInfoList)
		{
			pluginPanel.add(pluginPanel(pluginInfo, false, false));
		}

		panel.add(titleLabel("Available plugins"), BorderLayout.NORTH);
		panel.add(pluginPanel, BorderLayout.SOUTH);

		return panel;
	}

	private JPanel repository(String name, String url)
	{
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titleWrapper.setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR)
		));

		JLabel title = new JLabel();
		title.setText(name);
		title.setFont(normalFont);
		title.setBorder(null);
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		title.setPreferredSize(new Dimension(0, 24));
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 8, 0, 0));

		titleWrapper.add(title, BorderLayout.CENTER);

		JPanel titleActions = new JPanel(new BorderLayout(3, 0));
		titleActions.setBorder(new EmptyBorder(0, 0, 0, 8));
		titleActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel install = new JLabel();
		install.setIcon(DELETE_ICON);
		install.setToolTipText("Remove");
		install.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				externalPluginManager.removeRepository(name);
				buildPanel();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				install.setIcon(DELETE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				install.setIcon(DELETE_ICON);
			}
		});
		if (!name.equals("OpenOSRS"))
		{
			titleActions.add(install, BorderLayout.EAST);
		}
		titleWrapper.add(titleActions, BorderLayout.EAST);

		JLabel description = new JLabel();
		description.setText("<html>" + url.replace("https://raw.githubusercontent.com/", "").replace("/master/", "") + "</html>");
		description.setFont(smallFont);
		description.setBorder(null);
		description.setPreferredSize(new Dimension(0, 24));
		description.setForeground(Color.WHITE);
		description.setBorder(new EmptyBorder(0, 8, 0, 0));

		panel.add(titleWrapper, BorderLayout.NORTH);
		panel.add(description, BorderLayout.CENTER);

		return panel;
	}

	private JPanel pluginPanel(PluginInfo pluginInfo, boolean installed, boolean hideAction)
	{
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titleWrapper.setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR)
		));

		JLabel title = new JLabel();
		title.setText(pluginInfo.name);
		title.setFont(normalFont);
		title.setBorder(null);
		title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		title.setPreferredSize(new Dimension(0, 24));
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 8, 0, 0));

		titleWrapper.add(title, BorderLayout.CENTER);

		JPanel titleActions = new JPanel(new BorderLayout(3, 0));
		titleActions.setBorder(new EmptyBorder(0, 0, 0, 8));
		titleActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel install = new JLabel();
		install.setIcon(installed ? hideAction ? DELETE_ICON_GRAY : DELETE_ICON : ADD_ICON);
		if (!hideAction)
		{
			install.setToolTipText(installed ? "Uninstall" : "Install");
		}
		install.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (installed)
				{
					if (hideAction)
					{
						JOptionPane.showMessageDialog(null, "This plugin can't be uninstalled because one or more other plugins have a dependency on it.", "Error!", JOptionPane.ERROR_MESSAGE);
					}
					else
					{
						externalPluginManager.uninstall(pluginInfo.id);
						buildPanel();
					}
				}
				else
				{
					installPlugin(pluginInfo);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				install.setIcon(installed ? hideAction ? DELETE_HOVER_ICON_GRAY : DELETE_HOVER_ICON : ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				install.setIcon(installed ? hideAction ? DELETE_ICON_GRAY : DELETE_ICON : ADD_ICON);
			}
		});
		titleActions.add(install, BorderLayout.EAST);
		titleWrapper.add(titleActions, BorderLayout.EAST);

		JTextArea description = new JTextArea();
		description.setText(pluginInfo.description);
		description.setFont(smallFont);
		description.setBorder(null);
		description.setBorder(new EmptyBorder(0, 8, 0, 0));
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setHighlighter(null);
		description.setEnabled(false);
		description.setDisabledTextColor(Color.WHITE);

		panel.add(titleWrapper, BorderLayout.NORTH);
		panel.add(description, BorderLayout.CENTER);

		return panel;
	}

	private void installPlugin(PluginInfo pluginInfo)
	{
		try
		{
			externalPluginManager.install(pluginInfo.id);
		}
		catch (VerifyException ex)
		{
			JOptionPane.showMessageDialog(null, pluginInfo.name + " could not be installed, the hash could not be verified.", "Error!", JOptionPane.ERROR_MESSAGE);
		}

		buildPanel();
	}
}
