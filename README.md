# Olayinka personal website

A Spring Boot portfolio/blog with an admin page for editing content, adding
projects, and uploading images. Website content is stored in MySQL. Uploaded
image files are stored locally in `data/uploads/`, while their URLs are stored
in MySQL.

## Requirements

Install the following before starting:

- Java 17 or newer
- MySQL 8, or Docker with Docker Compose
- An internet connection the first time Maven downloads the dependencies

You do not need to install Maven separately because the project includes the
Maven Wrapper (`mvnw` and `mvnw.cmd`).

Check Java is available:

```bash
java -version
```

The output should show Java 17 or a newer version.

## 1. Open the project folder

Open a terminal and change to the folder containing this README and `pom.xml`:

```bash
cd /path/to/Personal-website
```

On this computer, the command is:

```bash
cd /home/winnie/Documents/Personal-website
```

## 2. Start MySQL

Choose either the Docker method or the locally installed MySQL method. Do not
run both on port 3306 at the same time.

### Option A: MySQL with Docker (recommended)

Start the included MySQL 8.4 container:

```bash
docker compose up -d mysql
```

Confirm that it is running:

```bash
docker compose ps
```

The `portfolio-mysql` service should eventually show as `healthy`. On the first
run, initialization can take a short while.

The container creates these development credentials automatically:

- Database: `portfolio`
- Username: `portfolio`
- Password: `portfolio`
- Port: `3306`

### Option B: Locally installed MySQL

Start the MySQL service. On many Linux systems:

```bash
sudo systemctl start mysql
```

Then create the database and development user using the included SQL script:

```bash
sudo mysql < database/setup.sql
```

The script creates the same `portfolio` database and `portfolio` user used by
the application's default configuration.

## 3. Set the admin password and run the application

Choose a private password for the admin page. Do not commit a real password to
this repository.

On Linux or macOS:

```bash
ADMIN_PASSWORD='choose-a-secure-password' ./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
$env:ADMIN_PASSWORD = "choose-a-secure-password"
.\mvnw.cmd spring-boot:run
```

Wait until the terminal reports that the application has started and Tomcat is
listening on port 8080. Keep this terminal open while using the website.

## 4. Open the website

Use these addresses in a browser:

- Public website: <http://localhost:8080>
- Admin login: <http://localhost:8080/admin>

Log in using the value supplied through `ADMIN_PASSWORD`.

From the admin page you can:

- Edit homepage, About, Services, Projects, and Contact content
- Click **+ Add project** to create another project
- Upload a homepage or project image using its own **Upload image** button
- Click **Save and publish** to store text changes in MySQL

Uploaded JPG and PNG files are automatically resized for the web. An individual
image must be no larger than 100 MB.

## Using different MySQL credentials

The application recognizes these environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC connection URL | `jdbc:mysql://localhost:3306/portfolio?...` |
| `DB_USERNAME` | MySQL username | `portfolio` |
| `DB_PASSWORD` | MySQL password | `portfolio` |
| `ADMIN_PASSWORD` | Password for `/admin` | `change-me` |
| `SITE_UPLOAD_DIR` | Uploaded-image directory | `data/uploads` |

Example using a different local MySQL account:

```bash
DB_URL='jdbc:mysql://localhost:3306/portfolio?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DB_USERNAME='your-user' \
DB_PASSWORD='your-database-password' \
ADMIN_PASSWORD='your-admin-password' \
./mvnw spring-boot:run
```

## Stop and restart

Stop the Spring Boot application by pressing `Ctrl+C` in its terminal.

If MySQL is running through Docker, stop the container with:

```bash
docker compose stop mysql
```

Start it again later with:

```bash
docker compose up -d mysql
```

To rebuild everything and start from freshly compiled classes:

```bash
ADMIN_PASSWORD='choose-a-secure-password' ./mvnw clean spring-boot:run
```

Running `clean` does not delete the MySQL database or uploaded images.

## Run the tests

The automated tests use an in-memory H2 database, so MySQL does not need to be
running for this command:

```bash
./mvnw clean test
```

## Build and run a JAR

Build the production JAR:

```bash
./mvnw clean package
```

After a successful build, run it with:

```bash
ADMIN_PASSWORD='choose-a-secure-password' java -jar target/wd-0.0.1-SNAPSHOT.jar
```

MySQL must already be running before the JAR starts.

## Deploy to Render

The included `render.yaml` deploys the application and a private MySQL 8.4
service in Render's Frankfurt region. It uses persistent disks so both the
database and images uploaded through the admin page survive redeploys.

1. Push this repository to GitHub, GitLab, or Bitbucket.
2. In Render, choose **New** → **Blueprint** and select the repository.
3. Render reads `render.yaml`. Enter a long, unique value for
   `ADMIN_PASSWORD` when prompted, then create the Blueprint.
4. Wait for `portfolio-mysql` to finish its first deploy, then wait for
   `olayinka-portfolio` to become live. Open its `onrender.com` URL.

The Blueprint uses paid `1c-2g` services because Render only provides
persistent disks on paid compute. Do not use a free web service for this app:
it loses uploaded images whenever it restarts or spins down.

`MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD` are generated by Render and shared
privately between the two services. Do not add them to the repository.

## Troubleshooting

### The application cannot connect to MySQL

Typical messages include `Communications link failure`, `Connection refused`,
or `Access denied`.

Check Docker MySQL:

```bash
docker compose ps
docker compose logs mysql
```

If using local MySQL, check its service:

```bash
sudo systemctl status mysql
```

Also verify that `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` match the MySQL
server you started.

### Port 8080 is already in use

Stop the other application using port 8080, or start this application on a
different port:

```bash
ADMIN_PASSWORD='choose-a-secure-password' ./mvnw spring-boot:run \
  -Dspring-boot.run.arguments=--server.port=8081
```

Then open <http://localhost:8081>.

### Port 3306 is already in use

This usually means local MySQL is already running while Docker is also trying
to use the same port. Use either local MySQL or Docker MySQL, not both.

### Changes are not visible

Stop the old application with `Ctrl+C`, run it again with `clean`, and refresh
the browser:

```bash
ADMIN_PASSWORD='choose-a-secure-password' ./mvnw clean spring-boot:run
```

### An image upload is too large

Use an image smaller than 100 MB. Text-only **Save and publish** requests do not
use the image-upload route.

## Data locations

- Editable content: MySQL table `portfolio.site_content`
- Uploaded images: `data/uploads/`
- Database creation script: `database/setup.sql`
- Docker database volume: `portfolio_mysql_data`

Do not delete the Docker volume or the `data/uploads/` directory if you want to
keep the current website data and uploaded images.
