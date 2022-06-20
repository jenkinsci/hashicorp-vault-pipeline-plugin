package io.jenkins.plugins.vault;

import com.bettercloud.vault.VaultConfig;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.datapipe.jenkins.vault.VaultAccessor;
import com.datapipe.jenkins.vault.configuration.GlobalVaultConfiguration;
import com.datapipe.jenkins.vault.configuration.VaultConfiguration;
import com.datapipe.jenkins.vault.credentials.VaultCredential;
import com.datapipe.jenkins.vault.exception.VaultPluginException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class VaultReadStep extends Step {
    private String path;
    private String key;
    private String credentialsId;
    private String vaultUrl;
    private String engineVersion;

    @DataBoundConstructor
    public VaultReadStep() {

    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    public void setKey(String key) {
        this.key = key;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    @DataBoundSetter
    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new VaultStepExecution(this, stepContext);
    }

    private static final class VaultStepExecution extends StepExecution {
        private static final long serialVersionUID = 1L;
        private transient final VaultReadStep step;

        private VaultStepExecution(VaultReadStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        private EnvVars getEnvironment() throws Exception {
            Run run = getContext().get(Run.class);
            TaskListener taskListener = getContext().get(TaskListener.class);
            return run.getEnvironment(taskListener);
        }

        private VaultAccessor getAccessor(Run<?, ?> run, TaskListener listener) throws Exception {
            EnvVars environment = getEnvironment();
            VaultConfiguration vaultConfiguration = GlobalVaultConfiguration.get().getConfiguration();
            String credentialsId = step.credentialsId == null || step.credentialsId.isEmpty() ? vaultConfiguration.getVaultCredentialId() : Util.replaceMacro(step.credentialsId, environment);
            String vaultUrl = step.vaultUrl == null || step.vaultUrl.isEmpty() ? vaultConfiguration.getVaultUrl() : Util.replaceMacro(step.vaultUrl, environment);

            listener.getLogger().append(String.format("using vault credentials \"%s\" and url \"%s\"", credentialsId, vaultUrl));

            VaultCredential credentials = CredentialsProvider.findCredentialById(credentialsId, VaultCredential.class, run);

            VaultConfig vaultConfig = vaultConfiguration.getVaultConfig();
            vaultConfig.address(vaultUrl);

            VaultAccessor vaultAccessor = new VaultAccessor(vaultConfig, credentials);
            vaultAccessor.init();

            vaultAccessor.init(vaultUrl, credentials);
            return vaultAccessor;
        }

        private int getEngineVersion(EnvVars environment) {
            if (step.engineVersion == null || step.engineVersion.isEmpty()) {
                VaultConfig vaultConfig = GlobalVaultConfiguration.get().getConfiguration().getVaultConfig();
                return vaultConfig.getGlobalEngineVersion();
            }

            return Integer.parseInt(Util.replaceMacro(step.engineVersion, environment));
        }

        @Override
        public boolean start() throws Exception {
            try {
                EnvVars environment = getEnvironment();
                String path = Util.replaceMacro(step.path, environment);
                Integer engineVersion = getEngineVersion(environment);
                String key = Util.replaceMacro(step.key, environment);
                String value = getAccessor(getContext().get(Run.class), getContext().get(TaskListener.class)).read(path, engineVersion).getData().get(key);
                getContext().onSuccess(value);
            } catch (VaultPluginException e) {
                getContext().onFailure(e);
            }
            return true;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<Class<?>>() {{
                add(Run.class);
                add(TaskListener.class);
            }};
        }

        @Override
        public String getFunctionName() {
            return "vault";
        }
    }
}
