package io.jenkins.plugins.vault;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.datapipe.jenkins.vault.VaultAccessor;
import com.datapipe.jenkins.vault.configuration.GlobalVaultConfiguration;
import com.datapipe.jenkins.vault.credentials.VaultCredential;
import com.datapipe.jenkins.vault.exception.VaultPluginException;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.GlobalConfiguration;

public class VaultReadStep extends Step {

    private String credentialsId;

    private String key;

    private String path;

    private String vaultUrl;

    private int engineVersion;

    @DataBoundConstructor
    public VaultReadStep() {
        // default constructor for creating the step, together with setters
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setKey(String key) {
        this.key = key;
    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    @DataBoundSetter
    public void setEngineVersion(int engineVersion) {
        this.engineVersion = engineVersion;
    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new VaultStepExecution(this, stepContext);
    }

    private static final class VaultStepExecution extends StepExecution {

        private static final long serialVersionUID = 1L;

        private final transient VaultReadStep step;

        private VaultStepExecution(VaultReadStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() throws Exception {
            try {
                EnvVars environment = getEnvironment();
                String value = getAccessor(getContext().get(Run.class), getContext().get(TaskListener.class)).read(Util.replaceMacro(step.path, environment), step.engineVersion)
                    .getData()
                    .get(Util.replaceMacro(step.key, environment));
                getContext().onSuccess(value);
            } catch (VaultPluginException e) {
                getContext().onFailure(e);
            }
            return true;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) {
            // no action required on stopping
        }

        private VaultAccessor getAccessor(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
            EnvVars environment = getEnvironment();
            GlobalVaultConfiguration vaultConfig = GlobalConfiguration.all().get(GlobalVaultConfiguration.class);
            String credentialsId = step.credentialsId == null || step.credentialsId.isEmpty() ? vaultConfig.getConfiguration().getVaultCredentialId() : Util.replaceMacro(step.credentialsId, environment);
            String vaultUrl = step.vaultUrl == null || step.vaultUrl.isEmpty() ? vaultConfig.getConfiguration().getVaultUrl() : Util.replaceMacro(step.vaultUrl, environment);

            listener.getLogger().append(String.format("using vault url \"%s\" and credentialsId \"%s\"", vaultUrl, credentialsId));

            VaultAccessor vaultAccessor = new VaultAccessor();
            VaultCredential credentials = CredentialsProvider.findCredentialById(credentialsId, VaultCredential.class, run);

            if (credentials != null) {
                listener.getLogger().append("credentials with id \"%s\" found, initializing with credentials");
                vaultAccessor.init(vaultUrl, credentials);
            } else {
                listener.getLogger().append("no credentials found, initializing without credentials");
                vaultAccessor.init(vaultUrl);
            }
            return vaultAccessor;
        }

        private EnvVars getEnvironment() throws IOException, InterruptedException {
            Run run = getContext().get(Run.class);
            TaskListener taskListener = getContext().get(TaskListener.class);
            return run.getEnvironment(taskListener);
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "vault";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            HashSet<Class<?>> requiredContext = new HashSet<>();
            requiredContext.add(Run.class);
            requiredContext.add(TaskListener.class);
            return requiredContext;
        }
    }
}
