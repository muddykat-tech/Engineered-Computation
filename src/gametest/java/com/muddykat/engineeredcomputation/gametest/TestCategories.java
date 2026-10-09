package com.muddykat.engineeredcomputation.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;

import com.muddykat.engineeredcomputation.EngineeredComputation;

@EventBusSubscriber(modid = EngineeredComputation.MODID)
public final class TestCategories
{
	private static final String PROPERTY = "ec.tests";

	private TestCategories()
	{
	}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event)
	{
		if(!(event.getServer() instanceof GameTestServer server))
			return;
		Map<String, String> classOfTest = new HashMap<>();
		Map<String, Set<String>> categories = categories(classOfTest);
		Set<String> uncategorised = new TreeSet<>();
		List<TestFunction> tests = testsOf(server);
		for(TestFunction test : tests)
			if(!classOfTest.containsKey(test.testName()))
				uncategorised.add(test.testName());
		EngineeredComputation.LOGGER.info("[tests] categories: {}", categories);
		if(!uncategorised.isEmpty())
			EngineeredComputation.LOGGER.warn("[tests] tests outside any @TestCategory class (always run): {}", uncategorised);

		String asked = System.getProperty(PROPERTY, "").trim().toLowerCase(Locale.ROOT);
		if(asked.isEmpty())
		{
			EngineeredComputation.LOGGER.info("[tests] running all {} tests", tests.size());
			return;
		}
		Set<String> wanted = Arrays.stream(asked.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(TreeSet::new));
		Set<String> unknown = new TreeSet<>(wanted);
		unknown.removeAll(categories.keySet());
		if(!unknown.isEmpty())
			throw new IllegalArgumentException("Unknown test categories "+unknown+" - there are "+categories.keySet());
		Set<String> classes = new TreeSet<>();
		wanted.forEach(category -> classes.addAll(categories.get(category)));
		tests.removeIf(test -> classOfTest.containsKey(test.testName())&&!classes.contains(classOfTest.get(test.testName())));
		EngineeredComputation.LOGGER.info("[tests] running {} tests in {}", tests.size(), wanted);
	}

	private static Map<String, Set<String>> categories(Map<String, String> classOfTest)
	{
		Map<String, Set<String>> categories = new TreeMap<>();
		String annotation = "L"+TestCategory.class.getName().replace('.', '/')+";";
		ModList.get().getAllScanData().stream()
				.flatMap(data -> data.getAnnotations().stream())
				.filter(data -> data.annotationType().getDescriptor().equals(annotation))
				.forEach(data -> {
					String name = data.clazz().getClassName();
					String simple = name.substring(name.lastIndexOf('.')+1);
					categoriesOf(data).forEach(category -> categories.computeIfAbsent(category, c -> new TreeSet<>())
							.add(simple));
					try
					{
						for(Method method : Class.forName(name, false, TestCategories.class.getClassLoader())
								.getDeclaredMethods())
							if(method.isAnnotationPresent(GameTest.class))
								classOfTest.put(method.getName().toLowerCase(Locale.ROOT), simple);
							else if(method.isAnnotationPresent(GameTestGenerator.class))
								for(Object test : (Collection<?>)method.invoke(null))
									classOfTest.put(((TestFunction)test).testName(), simple);
					}
					catch(ReflectiveOperationException e)
					{
						throw new IllegalStateException("Can't read test class "+name, e);
					}
				});
		return categories;
	}

	@SuppressWarnings("unchecked")
	private static List<String> categoriesOf(ModFileScanData.AnnotationData data)
	{
		Object value = data.annotationData().get("value");
		return value instanceof List<?> list? (List<String>)list: List.of(String.valueOf(value));
	}

	@SuppressWarnings("unchecked")
	private static List<TestFunction> testsOf(GameTestServer server)
	{
		try
		{
			Field field = GameTestServer.class.getDeclaredField("testFunctions");
			field.setAccessible(true);
			return (List<TestFunction>)field.get(server);
		}
		catch(ReflectiveOperationException e)
		{
			throw new IllegalStateException("Can't reach the game test server's tests to pick categories", e);
		}
	}
}
