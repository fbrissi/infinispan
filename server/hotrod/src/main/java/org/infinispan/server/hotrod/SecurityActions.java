package org.infinispan.server.hotrod;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletionStage;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listenable;
import org.infinispan.security.Security;
import org.infinispan.security.actions.AddCacheManagerListenerAction;
import org.infinispan.security.actions.GetCacheAction;
import org.infinispan.security.actions.GetCacheComponentRegistryAction;
import org.infinispan.security.actions.GetCacheConfigurationAction;
import org.infinispan.security.actions.GetCacheManagerConfigurationAction;
import org.infinispan.security.actions.GetGlobalComponentRegistryAction;
import org.infinispan.security.actions.RemoveListenerAsyncAction;
import org.infinispan.security.impl.SecureCacheImpl;

/**
 * SecurityActions for the org.infinispan.server.hotrod package.
 * <p>
 * Do not move. Do not change class and method visibility to avoid being called from other {@link
 * java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author Tristan Tarrant
 * @since 7.0
 */
final class SecurityActions {
   private static <T> T doPrivileged(PrivilegedAction<T> action) {
      if (System.getSecurityManager() != null) {
         return AccessController.doPrivileged(action);
      } else {
         return Security.doPrivileged(action);
      }
   }

   static ComponentRegistry getCacheComponentRegistry(final AdvancedCache<?, ?> cache) {
      GetCacheComponentRegistryAction action = new GetCacheComponentRegistryAction(cache);
      return doPrivileged(action);
   }

   static Configuration getCacheConfiguration(final AdvancedCache<?, ?> cache) {
      GetCacheConfigurationAction action = new GetCacheConfigurationAction(cache);
      return doPrivileged(action);
   }

   static DistributionManager getDistributionManager(AdvancedCache<?, ?> cache) {
      return doPrivileged(cache::getDistributionManager);
   }

   @SuppressWarnings("unchecked")
   static <K, V> Cache<K, V> getCache(final EmbeddedCacheManager cacheManager, String cacheName) {
      GetCacheAction action = new GetCacheAction(cacheManager, cacheName);
      return (Cache<K, V>) doPrivileged(action);
   }

   static GlobalComponentRegistry getGlobalComponentRegistry(final EmbeddedCacheManager cacheManager) {
      GetGlobalComponentRegistryAction action = new GetGlobalComponentRegistryAction(cacheManager);
      return doPrivileged(action);
   }

   static GlobalConfiguration getCacheManagerConfiguration(final EmbeddedCacheManager cacheManager) {
      return doPrivileged(new GetCacheManagerConfigurationAction(cacheManager));
   }

   static void addListener(EmbeddedCacheManager cacheManager, Object listener) {
      doPrivileged(new AddCacheManagerListenerAction(cacheManager, listener));
   }

   static CompletionStage<Void> removeListenerAsync(Listenable listenable, Object listener) {
      RemoveListenerAsyncAction action = new RemoveListenerAsyncAction(listenable, listener);
      return doPrivileged(action);
   }

   static <K, V> AdvancedCache<K, V> getUnwrappedCache(final AdvancedCache<K, V> cache) {
      if (cache instanceof SecureCacheImpl) {
         return doPrivileged(((SecureCacheImpl<K, V>) cache)::getDelegate);
      } else {
         return cache;
      }
   }

   static <K, V> AdvancedCache<K, V> anonymizeSecureCache(AdvancedCache<K, V> cache) {
      return doPrivileged(() -> cache.transform(SecurityActions::unsetSubject));
   }

   private static <K, V> AdvancedCache<K, V> unsetSubject(AdvancedCache<K, V> cache) {
      if (cache instanceof SecureCacheImpl) {
         return new SecureCacheImpl<>(getUnwrappedCache(cache));
      } else {
         return cache;
      }
   }
}
