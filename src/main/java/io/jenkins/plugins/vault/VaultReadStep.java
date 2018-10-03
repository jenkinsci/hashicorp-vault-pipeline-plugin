package io.jenkins.plugins.vault;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.datapipe.jenkins.vault.VaultAccessor;
import com.datapipe.jenkins.vault.configuration.GlobalVaultConfiguration;
import com.datapipe.jenkins.vault.credentials.VaultCredential;
import com.datapipe.jenkins.vault.exception.VaultPluginException;
import hudson.Extension;
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

        private VaultAccessor getAccessor(Run<?, ?> run, TaskListener listener) {
            GlobalVaultConfiguration vaultConfig = GlobalConfiguration.all().get(GlobalVaultConfiguration.class);
            String credentialsId = step.credentialsId == null || step.credentialsId.isEmpty() ? vaultConfig.getConfiguration().getVaultCredentialId() : step.credentialsId;
            String vaultUrl = step.vaultUrl == null || step.vaultUrl.isEmpty() ? vaultConfig.getConfiguration().getVaultUrl() : step.vaultUrl;

            listener.getLogger().append(String.format("using vault credentials \"%s\" and url \"%s\"", credentialsId, vaultUrl));

            VaultAccessor vaultAccessor = new VaultAccessor();
            vaultAccessor.init(vaultUrl);

            VaultCredential credentials = CredentialsProvider.findCredentialById(credentialsId, VaultCredential.class, run);

            if (credentials != null) {
                vaultAccessor.auth(credentials);
            }
            return vaultAccessor;
        }

        @Override
        public boolean start() throws Exception {
            try {
                String value = getAccessor(getContext().get(Run.class), getContext().get(TaskListener.class)).read(step.path).getData().get(step.key);
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
            return new HashSet<Class<?>>() {{ add(Run.class); add(TaskListener.class); }};
        }

        @Override
        public String getFunctionName() {
            return "vault";
        }
    }
}
