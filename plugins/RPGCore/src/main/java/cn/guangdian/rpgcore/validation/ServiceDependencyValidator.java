package cn.guangdian.rpgcore.validation;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;

import java.util.*;

public class ServiceDependencyValidator {

    private final RPGCore rpgCore;
    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public ServiceDependencyValidator(RPGCore rpgCore) {
        this.rpgCore = rpgCore;
    }

    public void registerDependency(Class<?> service, Class<?>... requiredServices) {
        dependencies.put(service, Arrays.asList(requiredServices));
    }

    public ValidationResult validate() {
        errors.clear();
        warnings.clear();

        ServiceRegistry registry = rpgCore.getServiceRegistry();
        if (registry == null) {
            errors.add("ServiceRegistry is null - RPGCore may not be initialized");
            return new ValidationResult(false, errors, warnings);
        }

        for (Map.Entry<Class<?>, List<Class<?>>> entry : dependencies.entrySet()) {
            Class<?> service = entry.getKey();
            List<Class<?>> required = entry.getValue();

            for (Class<?> requiredService : required) {
                Object serviceInstance = registry.getService(requiredService);
                if (serviceInstance == null) {
                    errors.add("Service " + service.getSimpleName() + " requires " +
                            requiredService.getSimpleName() + " but it is not registered");
                }
            }
        }

        validateCoreServices();

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void validateCoreServices() {
        if (rpgCore.getHttpClient() == null) {
            errors.add("HttpClient is null - network features may not work");
        }

        if (rpgCore.getScheduler() == null) {
            errors.add("Scheduler is null - scheduling features will not work");
        }

        if (rpgCore.getCacheProvider() == null) {
            errors.add("CacheProvider is null - caching features will not work");
        }

        if (rpgCore.getMiniMessageService() == null) {
            warnings.add("MiniMessageService is null - color features may not work");
        }

        if (rpgCore.getExternalServices() == null) {
            errors.add("ExternalServices is null - external integrations will not work");
        }

        if (rpgCore.getMessageService() == null) {
            warnings.add("MessageService is null - message features may not work");
        }
    }

    public void validateOnStartup() {
        ValidationResult result = validate();
        if (!result.isValid()) {
            for (String error : result.getErrors()) {
                rpgCore.getLogger().severe("[ServiceValidator] " + error);
            }
        }
        for (String warning : result.getWarnings()) {
            rpgCore.getLogger().warning("[ServiceValidator] " + warning);
        }
        if (result.isValid()) {
            rpgCore.getLogger().info("[ServiceValidator] All core services validated successfully");
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }
}
