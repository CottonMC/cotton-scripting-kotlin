package io.github.cottonmc.scriptingkt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class CottonScriptingKotlin implements ModInitializer {
	public static final String MODID = "cotton-scripting-kotlin";

	Logger logger = LogManager.getLogger("Cotton Scripting Kotlin");

	@Override
	public void onInitialize() {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByExtension("kts");
		if (engine == null) throw new RuntimeException("Kotlin JSR223 wasn't loaded! Crashing for dev purposes!");
		File stdlib = FabricLoader.getInstance().getGameDirectory().toPath().resolve(".kotlin/stdlib.jar").toFile();
		if (!stdlib.exists()) {
			try {
				stdlib.getParentFile().mkdirs();
				stdlib.createNewFile();
				Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer("fabric-language-kotlin");
				if (containerOpt.isPresent()) { //will always exist since it's required
					ModContainer container = containerOpt.get();
					Path stdLibPath = container.getPath("META-INF/jars/kotlin-stdlib-1.3.61.jar");
					FileOutputStream out = new FileOutputStream(stdlib, false);
					Files.copy(stdLibPath, out);
					logger.info("[Cotton Scripting Kotlin] Exported stdlib jar to {}!", stdlib.getPath());
				}
			} catch (IOException e) {
				logger.error("[Cotton Scripting Kotlin] Couldn't copy Kotlin standard lib! JSR-223 will not work!");
			}
		}
		System.setProperty("kotlin.java.stdlib.jar", stdlib.getAbsolutePath());
		File config = new File("compiler/cli/cli-common/resources/META-INF/extensions/compiler.xml");
		if (!config.exists()) {
			try {
				config.getParentFile().mkdirs();
				config.createNewFile();
				Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(MODID);
				if (containerOpt.isPresent()) { //will always exist since it's us
					ModContainer container = containerOpt.get();
					Path configPath = container.getPath("compiler.xml");
					FileOutputStream out = new FileOutputStream(config, false);
					Files.copy(configPath, out);
					logger.info("[Cotton Scripting Kotlin] Exported Kotlin compiler config to {}!", config.getPath());
				}
			} catch (IOException e) {
				logger.error("[Cotton Scripting Kotlin] Couldn't copy compiler config! JSR-223 will not work!");
			}
		}
		try {
			/* TODO:
			This won't work outside of the Cotton Scripting Kotlin dev environment.
			Kotlin's compiler uses a separate classpath, which means that it can't access code it needs.
			It also can't access p much any other code, so there goes the whole point of this.
			To fix it, I'd need to inject my own `KotlinJsr223JvmScriptEngineFactoryBase`,
			and use a resources file to override the original.
			Whacking at this for six hours just burnt me out on modding agin, so someone else can fix it if they want.
			 */
			engine.eval("println(\"[Cotton Scripting Kotlin|JSR223] Kotlin JSR223 is working!\")");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
