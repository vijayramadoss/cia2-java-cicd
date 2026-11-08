# Cloud setup guide (Azure + SonarQube Cloud + GitHub)

Do these steps **in order**. Run the commands in a **new PowerShell window**, so the newly
installed tools (`az`, `mvn`, `gitleaks`) are on the PATH.

Estimated time: about 45 minutes, including about 15 minutes of waiting for the AKS cluster.

---

## 1. Azure for Students (5 min)

1. Go to <https://azure.microsoft.com/free/students> and sign in with your college email.
2. Activate the offer ($100 credit, no credit card).
3. Sign in from PowerShell:

```powershell
az login
az account show --query "{name:name, id:id}" -o table
```

## 2. Create the Azure resources (20 min, mostly waiting)

Pick a **globally unique** registry name: lowercase letters and digits only, for example `cia2taskmgr` plus your roll number.

```powershell
$RG   = "rg-cia2-cicd"
$LOC  = "centralindia"              # if a policy blocks it, try southeastasia or eastus
$ACR  = "cia2taskmgr<rollno>"       # <-- change this
$AKS  = "aks-cia2"

# Register the resource providers (only needed once per subscription)
az provider register --namespace Microsoft.ContainerService --wait
az provider register --namespace Microsoft.ContainerRegistry --wait

# Resource group
az group create --name $RG --location $LOC

# Container registry (Basic tier, admin user on so GitHub Actions can push)
az acr create --resource-group $RG --name $ACR --sku Basic --admin-enabled true

# Kubernetes cluster: 1 small node, free control plane (~10-15 minutes)
az aks create --resource-group $RG --name $AKS `
  --node-count 1 --node-vm-size Standard_B2s `
  --tier free --generate-ssh-keys
```

> If `Standard_B2s` is not available in your region, use `Standard_B2als_v2` or `Standard_D2s_v3`.
> Azure for Students has a small vCPU quota; one 2-vCPU node fits inside it.

Check that the cluster works:

```powershell
az aks get-credentials --resource-group $RG --name $AKS --overwrite-existing
kubectl get nodes
```

## 3. Collect the values for GitHub (5 min)

```powershell
# Registry address and login (for the ACR_* settings)
az acr show -n $ACR --query loginServer -o tsv
az acr credential show -n $ACR --query "{username:username, password:passwords[0].value}" -o table

# Kubeconfig file for the pipeline (KUBE_CONFIG secret)
az aks get-credentials --resource-group $RG --name $AKS --admin --file kubeconfig-aks.yaml
notepad kubeconfig-aks.yaml       # copy the whole content
```

`kubeconfig-aks.yaml` is ignored by `.gitignore`. **Never commit it.** Gitleaks would block it anyway.
Delete the file after you have copied it into GitHub.

## 4. GitHub repository (5 min)

1. Create a new **public** repository called `cia2-java-cicd` on GitHub.
   Do **not** add a README, .gitignore or licence.
2. Open **Settings → Secrets and variables → Actions**.

**Secrets** tab → *New repository secret*:

| Name | Value |
|---|---|
| `ACR_USERNAME` | username from step 3 |
| `ACR_PASSWORD` | password from step 3 |
| `KUBE_CONFIG` | full content of `kubeconfig-aks.yaml` |
| `SONAR_TOKEN` | token from step 5 |

**Variables** tab → *New repository variable*:

| Name | Value |
|---|---|
| `ACR_LOGIN_SERVER` | e.g. `cia2taskmgr123.azurecr.io` |
| `SONAR_ORGANIZATION` | organization key from step 5 |
| `SONAR_PROJECT_KEY` | project key from step 5 |

## 5. SonarQube Cloud (10 min)

1. Go to <https://sonarcloud.io> → **Log in with GitHub**.
2. **+ → Create new organization → Import from GitHub**. Choose your account and the **Free plan**.
   Note the **organization key**.
3. **+ → Analyze new project** → select `cia2-java-cicd` → **Set up**.
   - "New code" definition: *Previous version*.
   - Choose analysis method **With GitHub Actions**. Note the **project key**,
     usually `<organization>_cia2-java-cicd`.
4. Important: **Administration → Analysis Method → turn OFF "Automatic Analysis"**.
   CI-based analysis and automatic analysis cannot both be on.
5. **My Account → Security → Generate token**. This is `SONAR_TOKEN`.

## 6. Push the code (2 min)

From the project folder:

```powershell
git remote add origin https://github.com/<your-username>/cia2-java-cicd.git
git push -u origin main
```

Open the **Actions** tab. The *CI/CD Pipeline* run starts automatically and takes about 6–8 minutes.
When it finishes, the **Deploy** job summary shows the application URL.

```powershell
kubectl get deployment,pods,service -n taskmanager -o wide
```

Open `http://<EXTERNAL-IP>` in the browser.

## 7. Change-and-redeploy demonstration

```powershell
# 1. Make a visible change
notepad src\main\resources\application.properties
#    change:  app.message=Version 1 - deployed by the CI/CD pipeline
#    to:      app.message=Version 2 - updated automatically by the pipeline

# 2. Commit and push (the Gitleaks pre-commit hook runs first)
git commit -am "Update welcome message to version 2"
git push

# 3. Watch the pods roll over to the new image
kubectl get pods -n taskmanager -w
```

Refresh the browser. The new message and the new commit SHA (in "Version") are shown.

## 8. Screenshots to take for the report

- [ ] Azure portal: resource group with the ACR and AKS resources
- [ ] AKS → Workloads: `taskmanager` deployment ready, pods **Running**
- [ ] ACR → Repositories → `taskmanager`: the image tags (commit SHAs + `latest`)
- [ ] `kubectl get deployment,pods,service -n taskmanager -o wide` output
- [ ] Browser showing the app at the public IP (before and after the change)
- [ ] GitHub Actions: a successful run with all 4 jobs green
- [ ] Gitleaks job log ("no leaks found") and the pre-commit hook blocking a fake secret
- [ ] SonarQube Cloud (and/or local `http://localhost:9000`) dashboard: quality gate **Passed**
- [ ] The second pipeline run triggered by the change commit

### Showing Gitleaks block a secret (for the viva)

The fake key is built from two parts so that this guide itself does not trip Gitleaks.

```powershell
"aws_access_key_id = " + "AKIA" + "Z7Q3EXAMPLE4QWRT" | Out-File demo-secret.txt
git add demo-secret.txt
git commit -m "test"          # blocked by the pre-commit hook
git reset demo-secret.txt; Remove-Item demo-secret.txt
```

## 9. Saving credit / cleaning up

```powershell
az aks stop  --resource-group rg-cia2-cicd --name aks-cia2    # stop when not demoing
az aks start --resource-group rg-cia2-cicd --name aks-cia2    # start again (~5 min)
az group delete --name rg-cia2-cicd --yes --no-wait           # delete everything after the viva
```

The public IP of the service can change after `az aks stop` / `start`.
