package com.muddykat.engineeredcomputation.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.forgespi.language.ModFileScanData;

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
			if(!classOfTest.containsKey(test.getTestName()))
				uncategorised.add(test.getTestName());
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
		int kept = retain(server, test -> !classOfTest.containsKey(test.getTestName())
				||classes.contains(classOfTest.get(test.getTestName())));
		EngineeredComputation.LOGGER.info("[tests] running {} tests in {}", kept, wanted);
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
									classOfTest.put(((TestFunction)test).getTestName(), simple);
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

	private static List<TestFunction> testsOf(GameTestServer server)
	{
		return batchesOf(server).stream().flatMap(batch -> batch.getTestFunctions().stream()).toList();
	}

	/** 1.20.1's server keeps batches of tests, not a list: rebuilds them with only the tests kept. */
	private static int retain(GameTestServer server, Predicate<TestFunction> keep)
	{
		List<GameTestBatch> batches = batchesOf(server);
		List<GameTestBatch> filtered = new ArrayList<>();
		for(GameTestBatch batch : batches)
		{
			List<TestFunction> tests = batch.getTestFunctions().stream().filter(keep).toList();
			if(!tests.isEmpty())
			{
				String name = tests.get(0).getBatchName();
				filtered.add(new GameTestBatch(batch.getName(), tests, GameTestRegistry.getBeforeBatchFunction(name),
						GameTestRegistry.getAfterBatchFunction(name)));
			}
		}
		batches.clear();
		batches.addAll(filtered);
		return filtered.stream().mapToInt(batch -> batch.getTestFunctions().size()).sum();
	}

	@SuppressWarnings("unchecked")
	private static List<GameTestBatch> batchesOf(GameTestServer server)
	{
		try
		{
			Field field = GameTestServer.class.getDeclaredField("testBatches");
			field.setAccessible(true);
			return (List<GameTestBatch>)field.get(server);
		}
		catch(ReflectiveOperationException e)
		{
			throw new IllegalStateException("Can't reach the game test server's tests to pick categories", e);
		}
	}
}
