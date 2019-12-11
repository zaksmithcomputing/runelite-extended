package net.runelite.client.plugins.openosrs.externals;

import com.google.gson.JsonSyntaxException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ExternalPluginChanged;
import net.runelite.client.events.ExternalPluginsLoaded;
import net.runelite.client.plugins.ExternalPluginManager;
import net.runelite.client.plugins.openosrs.OpenOSRSPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.pf4j.update.PluginInfo;
import org.pf4j.update.UpdateManager;
import org.pf4j.update.UpdateRepository;
import org.pf4j.update.VerifyException;

@Slf4j
public class PluginManagerPanel extends PluginPanel
{
	private static final JaroWinklerDistance DISTANCE = new JaroWinklerDistance();

	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;
	private static final ImageIcon FILTER_ICON;
	private static final ImageIcon FILTER_HOVER_ICON;
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

		final BufferedImage filterIcon =
			ImageUtil.recolorImage(
				ImageUtil.getResourceStreamFromClass(OpenOSRSPlugin.class, "filter.png"), ColorScheme.BRAND_BLUE
			);
		FILTER_ICON = new ImageIcon(filterIcon);
		FILTER_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(filterIcon, 0.53f));

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
	private final IconTextField searchBar = new IconTextField();
	private final List<PluginInfo> installedPluginsList = new ArrayList<>();
	private final List<PluginInfo> availablePluginsList = new ArrayList<>();
	private String filterMode = "All";
	private JPanel repositoriesPanel = new JPanel();
	private JPanel installedPluginsPanel = new JPanel();
	private JPanel availablePluginsPanel = new JPanel();
	private int scrollBarPosition;
	private JScrollBar scrollbar;
	private Set<String> deps;

	@Inject
	private PluginManagerPanel(ExternalPluginManager externalPluginManager, EventBus eventBus)
	{
		super(false);

		this.externalPluginManager = externalPluginManager;
		this.updateManager = externalPluginManager.getUpdateManager();

		eventBus.subscribe(ExternalPluginsLoaded.class, "loading-externals", (e) -> {
			log.info("EXTERNAL LOADED EVENT!");
			eventBus.unregister("loading-externals");
			eventBus.subscribe(ExternalPluginChanged.class, this, this::onExternalPluginChanged);
			onExternalPluginChanged(null);
		});

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				onSearchBarChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				onSearchBarChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				onSearchBarChanged();
			}
		});

		buildPanel();
	}

	private void buildPanel()
	{
		setLayout(new BorderLayout(0, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(titleBar(), BorderLayout.NORTH);
		add(wrapContainer(getContentPanels()), BorderLayout.CENTER);

		revalidate();
		repaint();
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

	private JPanel titleBar()
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel();
		JLabel addRepo = new JLabel(ADD_ICON);
		JLabel filter = new JLabel(FILTER_ICON);

		title.setText("External Plugin Manager");
		title.setForeground(Color.WHITE);

		filter.setToolTipText("Filter list");
		filter.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				List<String> choices = new ArrayList<String>()
				{{
					add("All");
					add("Repositories");
					add("All plugins");
					add("Installed plugins");
					add("All Available plugins");
				}};

				List<UpdateRepository> repositories = externalPluginManager.getRepositories();

				if (repositories.size() > 1)
				{
					for (UpdateRepository repository : repositories)
					{
						choices.add(repository.getId() + "'s plugins");
					}
				}

				String input = (String) JOptionPane.showInputDialog(null, "Choose now...",
					"Filter", JOptionPane.QUESTION_MESSAGE, null,
					choices.toArray(),
					choices.get(0));

				if (input != null)
				{
					filterMode = input;
				}
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				filter.setIcon(FILTER_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				filter.setIcon(FILTER_ICON);
			}
		});

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
				repositories();
				repositoriesPanel.revalidate();
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

		titleActions.add(filter, BorderLayout.WEST);
		titleActions.add(addRepo, BorderLayout.EAST);

		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.add(titleActions, BorderLayout.EAST);

		return titlePanel;
	}

	// Wrap the panel inside a scroll pane
	private JScrollPane wrapContainer(final JPanel container)
	{
		final JPanel wrapped = new JPanel(new BorderLayout());
		wrapped.add(container, BorderLayout.NORTH);

		final JScrollPane scroller = new JScrollPane(wrapped);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

		this.scrollbar = scroller.getVerticalScrollBar();

		return scroller;
	}

	private void onExternalPluginChanged(ExternalPluginChanged externalPluginChanged)
	{
		fetchPlugins();

		try
		{
			SwingUtil.syncExec(() -> {
				this.installedPlugins();
				this.availablePlugins();

				installedPluginsPanel.revalidate();
				availablePluginsPanel.revalidate();

				resetScrollValue();
			});

		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}

		resetScrollValue();
	}

	private void onSearchBarChanged()
	{
//		for (Component c : pluginPanels.getComponents())
//		{
//			if (!(c instanceof IconTextField))
//			{
//				pluginPanels.remove(c);
//			}
//		}
//
//		getAllPluginPanels();
//		revalidate();
//		repaint();
	}

	private JPanel getContentPanels()
	{
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 0, 5, 0);

		contentPanel.add(repositoriesPanel(), c);

		c.gridy++;
		contentPanel.add(installedPluginsPanel(), c);

		c.gridy++;
		contentPanel.add(availablePluginsPanel(), c);

		return contentPanel;
	}

	private JPanel repositoriesPanel()
	{
		JPanel installedRepositoriesPanel = new JPanel();
		installedRepositoriesPanel.setLayout(new BorderLayout(0, 5));
		installedRepositoriesPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
		installedRepositoriesPanel.add(titleLabel("Repositories"), BorderLayout.NORTH);
		installedRepositoriesPanel.add(repositoriesPanel, BorderLayout.CENTER);

		repositories();

		return installedRepositoriesPanel;
	}

	private JPanel installedPluginsPanel()
	{
		JPanel installedPluginsContainer = new JPanel();
		installedPluginsContainer.setLayout(new BorderLayout(0, 5));
		installedPluginsContainer.setBorder(new EmptyBorder(0, 10, 10, 10));
		installedPluginsContainer.add(titleLabel("Installed plugins"), BorderLayout.NORTH);
		installedPluginsContainer.add(installedPluginsPanel, BorderLayout.CENTER);

		return installedPluginsContainer;
	}

	private JPanel availablePluginsPanel()
	{
		JPanel availablePluginsContainer = new JPanel();
		availablePluginsContainer.setLayout(new BorderLayout(0, 5));
		availablePluginsContainer.setBorder(new EmptyBorder(0, 10, 10, 10));
		availablePluginsContainer.add(titleLabel("Available plugins"), BorderLayout.NORTH);
		availablePluginsContainer.add(availablePluginsPanel, BorderLayout.CENTER);

		return availablePluginsContainer;
	}

	private void repositories()
	{
		repositoriesPanel.removeAll();
		repositoriesPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		for (UpdateRepository repository : externalPluginManager.getRepositories())
		{
			String name = repository.getId();
			ExternalBox repositoryBox = new ExternalBox(name, repository.getUrl());

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 0, 0, 0);

			repositoriesPanel.add(repositoryBox, c);

			if (name.equals("OpenOSRS"))
			{
				repositoryBox.install.setVisible(false);
				continue;
			}

			repositoryBox.install.setIcon(DELETE_ICON);
			repositoryBox.install.setToolTipText("Remove");
			repositoryBox.install.addMouseListener(new MouseAdapter()
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
					repositoryBox.install.setIcon(DELETE_HOVER_ICON);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					repositoryBox.install.setIcon(DELETE_ICON);
				}
			});
		}
	}

	private void fetchPlugins()
	{
		List<PluginInfo> availablePlugins = null;
		List<PluginInfo> plugins = null;
		List<String> disabledPlugins = externalPluginManager.getDisabledPlugins();

		try
		{
			availablePlugins = updateManager.getAvailablePlugins();
			plugins = updateManager.getPlugins();
		}
		catch (JsonSyntaxException ex)
		{
			log.error(String.valueOf(ex));
		}

		if (availablePlugins == null || plugins == null)
		{
			JOptionPane.showMessageDialog(null, "The external plugin list could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		availablePluginsList.clear();
		installedPluginsList.clear();
		deps = externalPluginManager.getDependencies();

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
	}

	private void installedPlugins()
	{
		installedPluginsPanel.removeAll();
		installedPluginsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		for (PluginInfo pluginInfo : installedPluginsList)
		{
			ExternalBox pluginBox = new ExternalBox(pluginInfo);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 0, 0, 0);

			installedPluginsPanel.add(pluginBox, c);
			pluginInstallButton(pluginBox.install, pluginInfo, true, deps.contains(pluginInfo.id));
		}
	}

	private void availablePlugins()
	{
		availablePluginsPanel.removeAll();
		availablePluginsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		for (PluginInfo pluginInfo : availablePluginsList)
		{
			ExternalBox pluginBox = new ExternalBox(pluginInfo);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.gridy += 1;
			c.insets = new Insets(5, 0, 0, 0);

			availablePluginsPanel.add(pluginBox, c);
			pluginInstallButton(pluginBox.install, pluginInfo, false, false);
		}
	}

	boolean matchesSearchTerms(PluginInfo pluginInfo)
	{
		final String[] searchTerms = searchBar.getText().toLowerCase().split(" ");
		final String[] pluginTerms = (pluginInfo.name + " " + pluginInfo.description).toLowerCase().split("[/\\s]");
		for (String term : searchTerms)
		{
			if (Arrays.stream(pluginTerms).noneMatch((t) -> t.contains(term) ||
				DISTANCE.apply(t, term) > 0.9))
			{
				return false;
			}
		}
		return true;
	}

	private void pluginInstallButton(JLabel install, PluginInfo pluginInfo, boolean installed, boolean hideAction)
	{
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
				saveScrollValue();

				if (installed)
				{
					if (hideAction)
					{
						JOptionPane.showMessageDialog(null, "This plugin can't be uninstalled because one or more other plugins have a dependency on it.", "Error!", JOptionPane.ERROR_MESSAGE);
					}
					else
					{
						externalPluginManager.uninstall(pluginInfo.id);
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
	}

	private void saveScrollValue()
	{
		scrollBarPosition = scrollbar.getValue();
		log.info("Saving scroll value: {}", scrollBarPosition);
	}

	private void resetScrollValue()
	{
		scrollbar.setValue(scrollBarPosition);
		log.info("Resetting scroll value: {}", scrollBarPosition);
	}
}
