package net.runelite.client.eventbus;

<<<<<<< HEAD
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.sentry.Sentry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.Event;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.OpenOSRSConfig;
=======
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ReflectUtil;
>>>>>>> runelite/master

@Slf4j
@Singleton
public class EventBus implements EventBusInterface
{
	private final Map<Object, Object> subscriptionList = new HashMap<>();
	private final Map<Class<?>, Relay<Object>> subjectList = new HashMap<>();
	private final Map<Object, CompositeDisposable> subscriptionsMap = new HashMap<>();

	@Inject
	private OpenOSRSConfig openOSRSConfig;

	@Inject
	private ClientThread clientThread;

	@NonNull
	private <T extends Event> Relay<Object> getSubject(Class<T> eventClass)
	{
		return subjectList.computeIfAbsent(eventClass, k -> PublishRelay.create().toSerialized());
	}

	@NonNull
	private CompositeDisposable getCompositeDisposable(@NonNull Object object)
	{
		CompositeDisposable compositeDisposable = subscriptionsMap.get(object);
		if (compositeDisposable == null)
		{
			compositeDisposable = new CompositeDisposable();
			subscriptionsMap.put(object, compositeDisposable);
		}

		return compositeDisposable;
	}

	private <T> ObservableTransformer<T, T> applyTake(int until)
	{
		return observable -> until > 0 ? observable.take(until) : observable;
	}

	private Scheduler getScheduler(EventScheduler scheduler)
	{
		Scheduler subscribeScheduler;
		switch (scheduler)
		{
			case COMPUTATION:
				subscribeScheduler = Schedulers.computation();
				break;
			case IO:
				subscribeScheduler = Schedulers.io();
				break;
			case NEWTHREAD:
				subscribeScheduler = Schedulers.newThread();
				break;
			case SINGLE:
				subscribeScheduler = Schedulers.single();
				break;
			case TRAMPOLINE:
				subscribeScheduler = Schedulers.trampoline();
				break;
			case CLIENT:
				subscribeScheduler = Schedulers.from(clientThread);
				break;
			case DEFAULT:
			default:
				subscribeScheduler = null;
				break;
		}

		return subscribeScheduler;
	}

	private <T> ObservableTransformer<T, T> applyScheduler(EventScheduler eventScheduler, boolean subscribe)
	{
		Scheduler scheduler = getScheduler(eventScheduler);

<<<<<<< HEAD
		return observable -> scheduler == null ? observable : subscribe ? observable.subscribeOn(scheduler) : observable.observeOn(scheduler);
	}
=======
				try
				{
					final MethodHandles.Lookup caller = ReflectUtil.privateLookupIn(clazz);
					final MethodType subscription = MethodType.methodType(void.class, parameterClazz);
					final MethodHandle target = caller.findVirtual(clazz, method.getName(), subscription);
					final CallSite site = LambdaMetafactory.metafactory(
						caller,
						"invoke",
						MethodType.methodType(SubscriberMethod.class, clazz),
						subscription.changeParameterType(0, Object.class),
						target,
						subscription);

					final MethodHandle factory = site.getTarget();
					lambda = (SubscriberMethod) factory.bindTo(object).invokeExact();
				}
				catch (Throwable e)
				{
					log.warn("Unable to create lambda for method {}", method, e);
				}
>>>>>>> runelite/master

	@Override
	public <T extends Event> void subscribe(Class<T> eventClass, @NonNull Object lifecycle, @NonNull Consumer<T> action)
	{
		subscribe(eventClass, lifecycle, action, -1, EventScheduler.DEFAULT, EventScheduler.DEFAULT);
	}

	@Override
	public <T extends Event> void subscribe(Class<T> eventClass, @NonNull Object lifecycle, @NonNull Consumer<T> action, int takeUtil)
	{
		subscribe(eventClass, lifecycle, action, takeUtil, EventScheduler.DEFAULT, EventScheduler.DEFAULT);
	}

	@Override
	// Subscribe on lifecycle (for example from plugin startUp -> shutdown)
	public <T extends Event> void subscribe(Class<T> eventClass, @NonNull Object lifecycle, @NonNull Consumer<T> action, int takeUntil, @Nullable EventScheduler subscribe, @Nullable EventScheduler observe)
	{
		if (subscriptionList.containsKey(lifecycle) && eventClass.equals(subscriptionList.get(lifecycle)))
		{
			return;
		}

		Disposable disposable = getSubject(eventClass)
			.compose(applyTake(takeUntil))
			.filter(Objects::nonNull) // Filter out null objects, better safe than sorry
			.cast(eventClass) // Cast it for easier usage
			.doFinally(() -> unregister(lifecycle))
			.compose(applyScheduler(subscribe, true))
			.compose(applyScheduler(observe, false))
			.subscribe(action, error ->
			{
				log.error("Exception in eventbus", error);

				if (RuneLiteProperties.getLauncherVersion() != null && openOSRSConfig.shareLogs())
				{
					Sentry.capture(error);
				}
			});

		getCompositeDisposable(lifecycle).add(disposable);
		subscriptionList.put(lifecycle, eventClass);
	}

	@Override
	public void unregister(@NonNull Object lifecycle)
	{
		//We have to remove the composition from the map, because once you dispose it can't be used anymore
		CompositeDisposable compositeDisposable = subscriptionsMap.remove(lifecycle);
		subscriptionList.remove(lifecycle);
		if (compositeDisposable != null)
		{
			compositeDisposable.dispose();
		}
	}
<<<<<<< HEAD

	@Override
	public <T extends Event> void post(Class<? extends T> eventClass, @NonNull T event)
	{
		getSubject(eventClass).accept(event);
	}
=======
>>>>>>> runelite/master
}
