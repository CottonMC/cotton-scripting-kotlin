package io.github.cottonmc.scriptingkt;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class CottonScriptingKotlin implements ModInitializer {

	@Override
	public void onInitialize() {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByExtension("kts");
		if (engine == null) throw new RuntimeException("Kotlin JSR223 wasn't loaded! Crashing for dev purposes!");
	}
}
