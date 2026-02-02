# Architecture Documentation

## System Overview

The Health Management System is built using a layered architecture following Domain-Driven Design (DDD) principles with Spring Boot as the foundation.

## High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Clients                                    │
│         (Web Browser, Mobile App, External APIs)                       │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Load Balancer (ALB)                             │
│                    SSL/TLS Termination, Routing                        │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        API Gateway Layer                               │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                    Spring Security Filter Chain                   │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐ │ │
│  │  │   CORS     │─▶│    JWT     │─▶│   Auth     │─▶│  Request   │ │ │
│  │  │   Filter   │  │   Filter   │  │  Provider  │  │  Handler   │ │ │
│  │  └────────────┘  └────────────┘  └────────────┘  └────────────┘ │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Application Layer                               │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │                      REST Controllers                           │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ │   │
│  │  │  Auth    │ │ Patient  │ │  Doctor  │ │Appointment│ │Medical│ │   │
│  │  │Controller│ │Controller│ │Controller│ │Controller │ │Record │ │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────┘ │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                    │                                   │
│                                    ▼                                   │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │                      Service Layer                              │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ │   │
│  │  │  Auth    │ │ Patient  │ │  Doctor  │ │Appointment│ │Medical│ │   │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service   │ │Record │ │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────┘ │   │
│  └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Data Access Layer                               │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │                    JPA Repositories                             │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ │   │
│  │  │   User   │ │ Patient  │ │  Doctor  │ │Appointment│ │Medical│ │   │
│  │  │   Repo   │ │   Repo   │ │   Repo   │ │   Repo    │ │Repo   │ │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────┘ │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                    │                                   │
│                              Hibernate ORM                             │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL Database                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │  users  │ │ roles   │ │patients │ │ doctors │ │   appointments  │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────────────┘  │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐  │
│  │ medical_records │ │ refresh_tokens  │ │      user_roles         │  │
│  └─────────────────┘ └─────────────────┘ └─────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

## Domain Model (Entity Relationship Diagram)

```
┌──────────────┐       ┌──────────────┐
│     User     │       │     Role     │
├──────────────┤       ├──────────────┤
│ id           │       │ id           │
│ email        │◄─────▶│ name         │
│ password     │  M:N  │ (USER,ADMIN, │
│ firstName    │       │  SUPER_ADMIN)│
│ lastName     │       └──────────────┘
│ enabled      │
└──────────────┘
        │
        │ 1:N
        ▼
┌──────────────┐
│ RefreshToken │
├──────────────┤
│ id           │
│ token        │
│ expiryDate   │
│ revoked      │
└──────────────┘

┌──────────────┐                    ┌──────────────┐
│   Patient    │                    │    Doctor    │
├──────────────┤                    ├──────────────┤
│ id           │                    │ id           │
│ firstName    │                    │ firstName    │
│ lastName     │                    │ lastName     │
│ email        │                    │ email        │
│ phoneNumber  │                    │ phoneNumber  │
│ dateOfBirth  │                    │ licenseNumber│
│ gender       │                    │specialization│
│ address      │                    │ department   │
│ nationalId   │                    │ yearsOfExp   │
│ bloodType    │                    │ isAvailable  │
│ emergContact │                    └──────────────┘
└──────────────┘                           │
        │                                  │
        │ 1:N                         1:N  │
        ▼                                  ▼
┌────────────────────────────────────────────────┐
│                  Appointment                    │
├────────────────────────────────────────────────┤
│ id                                             │
│ patient_id (FK)                                │
│ doctor_id (FK)                                 │
│ appointmentDateTime                            │
│ status (SCHEDULED, CONFIRMED, COMPLETED, etc.) │
│ appointmentType (CONSULTATION, FOLLOW_UP, etc.)│
│ reasonForVisit                                 │
│ notes                                          │
│ createdAt, updatedAt                           │
└────────────────────────────────────────────────┘
        │
        │ 1:1
        ▼
┌────────────────────────────────────────────────┐
│                 MedicalRecord                   │
├────────────────────────────────────────────────┤
│ id                                             │
│ patient_id (FK)                                │
│ doctor_id (FK)                                 │
│ appointment_id (FK, optional)                  │
│ recordDate                                     │
│ diagnosis                                      │
│ symptoms                                       │
│ treatment                                      │
│ prescription                                   │
│ labResults                                     │
│ notes                                          │
│ followUpDate                                   │
│ createdAt, updatedAt                           │
└────────────────────────────────────────────────┘
```

## Security Architecture

### Authentication Flow

```
┌────────┐                ┌────────────┐              ┌──────────────┐
│ Client │                │  API       │              │   Database   │
└────┬───┘                └─────┬──────┘              └──────┬───────┘
     │                          │                            │
     │  POST /auth/login        │                            │
     │  {email, password}       │                            │
     │─────────────────────────▶│                            │
     │                          │  Find user by email        │
     │                          │───────────────────────────▶│
     │                          │                            │
     │                          │  User + hashed password    │
     │                          │◀───────────────────────────│
     │                          │                            │
     │                          │  Verify password (BCrypt)  │
     │                          │                            │
     │                          │  Generate JWT tokens       │
     │                          │                            │
     │                          │  Store refresh token       │
     │                          │───────────────────────────▶│
     │                          │                            │
     │  {accessToken,           │                            │
     │   refreshToken}          │                            │
     │◀─────────────────────────│                            │
     │                          │                            │
```

### JWT Token Structure

```
Header (Algorithm & Token Type)
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Claims)
{
  "sub": "user@example.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1706000000,
  "exp": 1706000900
}

Signature
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| `USER` | Read patients, doctors, appointments, medical records |
| `ADMIN` | All USER permissions + Create, Update operations |
| `SUPER_ADMIN` | All ADMIN permissions + Delete operations |

## Infrastructure Architecture (AWS)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              AWS Cloud                                   │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         Region: us-east-1                          │  │
│  │                                                                    │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │                        VPC (10.0.0.0/16)                     │  │  │
│  │  │                                                              │  │  │
│  │  │  ┌──────────────────┐      ┌──────────────────┐             │  │  │
│  │  │  │  Public Subnet   │      │  Public Subnet   │             │  │  │
│  │  │  │   10.0.1.0/24    │      │   10.0.2.0/24    │             │  │  │
│  │  │  │     (AZ-a)       │      │     (AZ-b)       │             │  │  │
│  │  │  │  ┌───────────┐   │      │  ┌───────────┐   │             │  │  │
│  │  │  │  │    ALB    │   │      │  │  NAT GW   │   │             │  │  │
│  │  │  │  └───────────┘   │      │  └───────────┘   │             │  │  │
│  │  │  └──────────────────┘      └──────────────────┘             │  │  │
│  │  │            │                       │                         │  │  │
│  │  │            ▼                       ▼                         │  │  │
│  │  │  ┌──────────────────┐      ┌──────────────────┐             │  │  │
│  │  │  │  Private Subnet  │      │  Private Subnet  │             │  │  │
│  │  │  │   10.0.3.0/24    │      │   10.0.4.0/24    │             │  │  │
│  │  │  │     (AZ-a)       │      │     (AZ-b)       │             │  │  │
│  │  │  │                  │      │                  │             │  │  │
│  │  │  │  ┌────────────┐  │      │  ┌────────────┐  │             │  │  │
│  │  │  │  │  EKS Pod   │  │      │  │  EKS Pod   │  │             │  │  │
│  │  │  │  │ (Fargate)  │  │      │  │ (Fargate)  │  │             │  │  │
│  │  │  │  └────────────┘  │      │  └────────────┘  │             │  │  │
│  │  │  └──────────────────┘      └──────────────────┘             │  │  │
│  │  │                                                              │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                    │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │  │
│  │  │      ECR        │  │ Secrets Manager │  │   CloudWatch    │   │  │
│  │  │  (Container     │  │  (DB creds,     │  │   (Logs &       │   │  │
│  │  │   Registry)     │  │   JWT secret)   │  │   Metrics)      │   │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘   │  │
│  │                                                                    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Deployment Pipeline

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Commit    │───▶│    Build    │───▶│    Test     │───▶│    Push     │
│   (GitLab)  │    │   (Maven)   │    │ (JUnit/TC)  │    │   (ECR)     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                                │
                                                                ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│    Prod     │◀───│   Staging   │◀───│     Dev     │◀───│   Deploy    │
│   (Manual)  │    │   (Manual)  │    │   (Auto)    │    │ (Kustomize) │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

## Technology Decisions

### Why Spring Boot 3.x?
- Native support for Java 21 virtual threads
- GraalVM native image support for faster startup
- Modern Spring Security 6 with improved configuration
- Active community and enterprise support

### Why JWT over Sessions?
- Stateless authentication enables horizontal scaling
- No server-side session storage required
- Works well with microservices and API-first architecture
- Mobile and SPA friendly

### Why PostgreSQL?
- ACID compliance for healthcare data integrity
- Rich JSON support for flexible medical records
- Excellent performance with proper indexing
- Mature ecosystem with strong tooling

### Why EKS with Fargate?
- Serverless compute reduces operational overhead
- Pay-per-use pricing model
- Automatic security patching
- Built-in high availability
- Seamless integration with AWS services

### Why Terraform?
- Infrastructure as Code enables version control
- Multi-environment consistency
- Reproducible deployments
- Large module ecosystem
- State management for drift detection
