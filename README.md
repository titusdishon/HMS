# HMS - Health Management System

[![CI/CD Pipeline](https://github.com/titusdishon/HMS/actions/workflows/ci.yml/badge.svg)](https://github.com/titusdishon/HMS/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A production-ready Spring Boot REST API for managing healthcare operations including patients, doctors, appointments, and medical records.

## Features

- **Patient Management**: CRUD operations for patient records with blood type, emergency contacts
- **Doctor Management**: Manage doctors with specializations and availability tracking
- **Appointment Scheduling**: Book, reschedule, and manage appointments with status tracking
- **Medical Records**: Track diagnoses, treatments, prescriptions, and follow-ups
- **Authentication & Authorization**: JWT-based auth with role-based access control (RBAC)
- **API Documentation**: Interactive Swagger UI with OpenAPI 3.0 specification
- **Cloud Native**: Containerized with Docker, deployable to AWS EKS with Fargate

## Technology Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.5.5 |
| Language | Java 21 |
| Database | PostgreSQL 16 |
| Security | Spring Security 6, JWT |
| Documentation | SpringDoc OpenAPI 2.8 |
| Testing | JUnit 5, TestContainers, REST Assured |
| Containerization | Docker, Kubernetes |
| Infrastructure | Terraform, AWS EKS, Fargate |
| CI/CD | GitHub Actions |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
│                    (Web, Mobile, Third-party)                   │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AWS Application Load Balancer               │
│                         (HTTPS Termination)                      │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Amazon EKS Cluster                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Fargate Pods                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │              Health Management System                │  │  │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │  │
│  │  │  │ Controllers │──│  Services   │──│Repositories │  │  │  │
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘  │  │  │
│  │  │         │                                    │       │  │  │
│  │  │         ▼                                    ▼       │  │  │
│  │  │  ┌─────────────┐                    ┌─────────────┐  │  │  │
│  │  │  │ JWT Filter  │                    │  JPA/ORM    │  │  │  │
│  │  │  └─────────────┘                    └─────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Amazon RDS (PostgreSQL)                      │
│                      or PostgreSQL Container                     │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Authentication
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/v1/auth/register` | Register new user | Public |
| POST | `/api/v1/auth/login` | Login and get tokens | Public |
| POST | `/api/v1/auth/refresh` | Refresh access token | Public |
| POST | `/api/v1/auth/logout` | Logout and invalidate tokens | Authenticated |

### Patients
| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/v1/patients` | List all patients | USER, ADMIN |
| GET | `/api/v1/patients/{id}` | Get patient by ID | USER, ADMIN |
| POST | `/api/v1/patients` | Create patient | ADMIN |
| PUT | `/api/v1/patients/{id}` | Update patient | ADMIN |
| DELETE | `/api/v1/patients/{id}` | Delete patient | SUPER_ADMIN |

### Doctors
| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/v1/doctors` | List all doctors | USER, ADMIN |
| GET | `/api/v1/doctors/{id}` | Get doctor by ID | USER, ADMIN |
| GET | `/api/v1/doctors/specialization/{spec}` | Filter by specialization | USER, ADMIN |
| GET | `/api/v1/doctors/available` | List available doctors | USER, ADMIN |
| POST | `/api/v1/doctors` | Create doctor | ADMIN |
| PUT | `/api/v1/doctors/{id}` | Update doctor | ADMIN |
| DELETE | `/api/v1/doctors/{id}` | Delete doctor | SUPER_ADMIN |

### Appointments
| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/v1/appointments` | List all appointments | USER, ADMIN |
| GET | `/api/v1/appointments/{id}` | Get appointment by ID | USER, ADMIN |
| POST | `/api/v1/appointments` | Create appointment | ADMIN |
| PATCH | `/api/v1/appointments/{id}/status` | Update status | ADMIN |
| PATCH | `/api/v1/appointments/{id}/cancel` | Cancel appointment | ADMIN |
| DELETE | `/api/v1/appointments/{id}` | Delete appointment | SUPER_ADMIN |

### Medical Records
| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/v1/medical-records` | List all records | USER, ADMIN |
| GET | `/api/v1/medical-records/{id}` | Get record by ID | USER, ADMIN |
| GET | `/api/v1/medical-records/patient/{id}` | Get by patient | USER, ADMIN |
| POST | `/api/v1/medical-records` | Create record | ADMIN |
| PUT | `/api/v1/medical-records/{id}` | Update record | ADMIN |
| DELETE | `/api/v1/medical-records/{id}` | Delete record | SUPER_ADMIN |

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/titusdishon/HMS.git
   cd HMS
   ```

2. **Start PostgreSQL**
   ```bash
   docker compose up -d
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the application**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

### Using Docker

```bash
# Build and run everything
docker compose --profile app up -d

# View logs
docker compose logs -f app
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | Database JDBC URL | `jdbc:postgresql://localhost:5332/devdishon` |
| `DB_USERNAME` | Database username | `devdishon` |
| `DB_PASSWORD` | Database password | `geek36873` |
| `JWT_SECRET` | JWT signing key (base64) | - |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | `604800000` (7 days) |

### Application Profiles

| Profile | Description |
|---------|-------------|
| `default` | Local development with H2/PostgreSQL |
| `docker` | Docker container configuration |
| `test` | Test configuration with TestContainers |
| `prod` | Production settings |

## Testing

```bash
# Run all tests
./mvnw verify

# Run unit tests only
./mvnw test

# Run with coverage report
./mvnw verify jacoco:report
open target/site/jacoco/index.html
```

## Deployment

### AWS EKS with Fargate

See [docs/deployment.md](docs/deployment.md) for detailed AWS deployment instructions.

```bash
# Initialize Terraform
cd terraform/environments/dev
terraform init

# Plan and apply
terraform plan -out=tfplan
terraform apply tfplan

# Deploy to Kubernetes
kubectl apply -k k8s/overlays/dev
```

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/devdishon/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── security/        # JWT and security
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # Test classes
├── terraform/                   # Infrastructure as Code
├── k8s/                         # Kubernetes manifests
├── docs/                        # Documentation
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/   # GitHub Actions CI/CD
└── pom.xml
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, please open an issue in the GitHub repository or contact the development team.
