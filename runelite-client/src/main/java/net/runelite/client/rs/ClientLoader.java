/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2018 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.rs;

import com.google.common.io.ByteStreams;
import java.applet.Applet;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
<<<<<<< HEAD
import net.runelite.client.ui.RuneLiteSplashScreen;
=======
import net.runelite.client.RuneLiteProperties;
import static net.runelite.client.rs.ClientUpdateCheckMode.AUTO;
import static net.runelite.client.rs.ClientUpdateCheckMode.NONE;
import static net.runelite.client.rs.ClientUpdateCheckMode.VANILLA;
import net.runelite.client.ui.FatalErrorDialog;
import net.runelite.client.ui.SplashScreen;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
>>>>>>> runelite/master

@Slf4j
public class ClientLoader implements Supplier<Applet>
{
	private final ClientUpdateCheckMode updateCheckMode;
	private Object client = null;

	public ClientLoader(ClientUpdateCheckMode updateCheckMode)
	{
		this.updateCheckMode = updateCheckMode;
	}

	@Override
	public synchronized Applet get()
	{
		if (client == null)
		{
			client = doLoad();
		}

		if (client instanceof Throwable)
		{
			throw new RuntimeException((Throwable) client);
		}
		return (Applet) client;
	}

	private Object doLoad()
	{
		try
		{
			RuneLiteSplashScreen.stage(.2, "Fetching applet viewer config");

			final RSConfig config = ClientConfigLoader.fetch()
				.retry(50)
				.blockingGet();

			switch (updateCheckMode)
			{
				case AUTO:
				default:
					return loadRLPlus(config);
				case VANILLA:
					return loadVanilla(config);
				case NONE:
					return null;
				case RSPS:
					RuneLite.allowPrivateServer = true;
					return loadRLPlus(config);
			}
		}
		catch (IOException | InstantiationException | IllegalAccessException e)
		{
			log.error("Error loading RS!", e);
			return null;
		}
<<<<<<< HEAD
		catch (ClassNotFoundException e)
		{
			RuneLiteSplashScreen.setError("Unable to load client", "Class not found. This means you"
				+ " are not running OpenOSRS with Gradle as the injected client"
				+ " is not in your classpath.");

			log.error("Error loading RS!", e);
			return null;
=======
	}

	private void downloadConfig() throws IOException
	{
		HttpUrl url = HttpUrl.parse(RuneLiteProperties.getJavConfig());
		IOException err = null;
		for (int attempt = 0; attempt < NUM_ATTEMPTS; attempt++)
		{
			try
			{
				config = ClientConfigLoader.fetch(url);

				if (Strings.isNullOrEmpty(config.getCodeBase()) || Strings.isNullOrEmpty(config.getInitialJar()) || Strings.isNullOrEmpty(config.getInitialClass()))
				{
					throw new IOException("Invalid or missing jav_config");
				}

				return;
			}
			catch (IOException e)
			{
				log.info("Failed to get jav_config from host \"{}\" ({})", url.host(), e.getMessage());
				String host = hostSupplier.get();
				url = url.newBuilder().host(host).build();
				err = e;
			}
		}

		log.info("Falling back to backup client config");

		try
		{
			RSConfig backupConfig = ClientConfigLoader.fetch(HttpUrl.parse(RuneLiteProperties.getJavConfigBackup()));

			if (Strings.isNullOrEmpty(backupConfig.getCodeBase()) || Strings.isNullOrEmpty(backupConfig.getInitialJar()) || Strings.isNullOrEmpty(backupConfig.getInitialClass()))
			{
				throw new IOException("Invalid or missing jav_config");
			}

			if (Strings.isNullOrEmpty(backupConfig.getRuneLiteGamepack()))
			{
				throw new IOException("Backup config does not have RuneLite gamepack url");
			}

			// Randomize the codebase
			String codebase = hostSupplier.get();
			backupConfig.setCodebase("http://" + codebase + "/");
			config = backupConfig;
		}
		catch (IOException ex)
		{
			throw err; // use error from Jagex's servers
>>>>>>> runelite/master
		}
	}

	private static Applet loadRLPlus(final RSConfig config)
		throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		RuneLiteSplashScreen.stage(.465, "Starting Open Old School RuneScape");

		ClassLoader rsClassLoader = new ClassLoader(ClientLoader.class.getClassLoader())
		{
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException
			{
				String path = name.replace('.', '/').concat(".class");
				InputStream inputStream = ClientLoader.class.getResourceAsStream(path);
				if (inputStream == null)
				{
					throw new ClassNotFoundException(name + " " + path);
				}
				byte[] data;
				try
				{
<<<<<<< HEAD
					data = ByteStreams.toByteArray(inputStream);
=======
					vanillaCacheIsInvalid = true;
				}
			}
			catch (Exception e)
			{
				log.info("Failed to read the vanilla cache: {}", e.toString());
				vanillaCacheIsInvalid = true;
			}
			vanilla.position(0);

			// Start downloading the vanilla client
			HttpUrl url;
			if (config.getRuneLiteGamepack() != null)
			{
				// If we are using the backup config, use our own gamepack and ignore the codebase
				url = HttpUrl.parse(config.getRuneLiteGamepack());
			}
			else
			{
				String codebase = config.getCodeBase();
				String initialJar = config.getInitialJar();
				url = HttpUrl.parse(codebase + initialJar);
			}

			for (int attempt = 0; ; attempt++)
			{
				Request request = new Request.Builder()
					.url(url)
					.build();

				try (Response response = RuneLiteAPI.CLIENT.newCall(request).execute())
				{
					// Its important to not close the response manually - this should be the only close or
					// try-with-resources on this stream or it's children

					int length = (int) response.body().contentLength();
					if (length < 0)
					{
						length = 3 * 1024 * 1024;
					}
					else
					{
						if (!vanillaCacheIsInvalid && vanilla.size() != length)
						{
							// The zip trailer filetab can be missing and the ZipInputStream will not notice
							log.info("Vanilla cache is the wrong size");
							vanillaCacheIsInvalid = true;
						}
					}
					final int flength = length;
					TeeInputStream copyStream = new TeeInputStream(new CountingInputStream(response.body().byteStream(),
						read -> SplashScreen.stage(.05, .35, null, "Downloading Old School RuneScape", read, flength, true)));

					// Save the bytes from the mtime test so we can write it to disk
					// if the test fails, or the cache doesn't verify
					ByteArrayOutputStream preRead = new ByteArrayOutputStream();
					copyStream.setOut(preRead);

					JarInputStream networkJIS = new JarInputStream(copyStream);

					// Get the mtime from the first entry so check it against the cache
					{
						JarEntry je = networkJIS.getNextJarEntry();
						networkJIS.skip(Long.MAX_VALUE);
						verifyJarEntry(je, jagexCertificateChain);
						long vanillaClientMTime = je.getLastModifiedTime().toMillis();
						if (!vanillaCacheIsInvalid && vanillaClientMTime != vanillaCacheMTime)
						{
							log.info("Vanilla cache is out of date: {} != {}", vanillaClientMTime, vanillaCacheMTime);
							vanillaCacheIsInvalid = true;
						}
					}

					// the mtime matches so the cache is probably up to date, but just make sure its fully
					// intact before closing the server connection
					if (!vanillaCacheIsInvalid)
					{
						try
						{
							// as with the request stream, its important to not early close vanilla too
							JarInputStream vanillaCacheTest = new JarInputStream(Channels.newInputStream(vanilla));
							verifyWholeJar(vanillaCacheTest, jagexCertificateChain);
						}
						catch (Exception e)
						{
							log.warn("Failed to verify the vanilla cache", e);
							vanillaCacheIsInvalid = true;
						}
					}

					if (vanillaCacheIsInvalid)
					{
						// the cache is not up to date, commit our peek to the file and write the rest of it, while verifying
						vanilla.position(0);
						OutputStream out = Channels.newOutputStream(vanilla);
						out.write(preRead.toByteArray());
						copyStream.setOut(out);
						verifyWholeJar(networkJIS, jagexCertificateChain);
						copyStream.skip(Long.MAX_VALUE); // write the trailer to the file too
						out.flush();
						vanilla.truncate(vanilla.position());
					}
					else
					{
						log.info("Using cached vanilla client");
					}
					return;
>>>>>>> runelite/master
				}
				catch (IOException e)
				{
					e.printStackTrace();
					RuneLiteSplashScreen.setError("Failed to load!", "Failed to load class: " + name + " " + path);
					throw new RuntimeException("Failed to load class: " + name + " " + path);
				}
				return defineClass(name, data, 0, data.length);
			}
		};
		Class<?> clientClass = rsClassLoader.loadClass("client");
		return loadFromClass(config, clientClass);
	}

	private static Applet loadVanilla(final RSConfig config)
		throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		RuneLiteSplashScreen.stage(.465, "Starting Vanilla Old School RuneScape");

		final String codebase = config.getCodeBase();
		final String initialJar = config.getInitialJar();
		final String initialClass = config.getInitialClass();
		final URL url = new URL(codebase + initialJar);

		// Must set parent classloader to null, or it will pull from
		// this class's classloader first
		final URLClassLoader classloader = new URLClassLoader(new URL[]{url}, null);
		final Class<?> clientClass = classloader.loadClass(initialClass);
		return loadFromClass(config, clientClass);
	}

	private static Applet loadFromClass(final RSConfig config, final Class<?> clientClass)
		throws IllegalAccessException, InstantiationException
	{
		final Applet rs = (Applet) clientClass.newInstance();
		rs.setStub(new RSAppletStub(config));
		return rs;
	}
}
