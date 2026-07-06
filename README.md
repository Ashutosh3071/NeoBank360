[README.md](https://github.com/user-attachments/files/29686520/README.md)
# NeoBank360 🏦

**Your Money, Reimagined.**

NeoBank360 is a full-stack digital banking platform that lets users open accounts instantly, transfer funds seamlessly, track budgets in real time, manage loans and bills, and earn rewards — all from one powerful dashboard.
<img width="1918" height="912" alt="Screenshot 2026-07-06 085821" src="https://github.com/user-attachments/assets/e68749d3-b1d3-446c-ba29-b7d8a031f535" />

---

## ✨ Features

- **Authentication & Security** – JWT-based login/register with role-based access (User/Admin)
- **Accounts & Transfers** – Instant account creation and seamless fund transfers
- **Transactions** – Full transaction history with filtering
- **Budgeting** – Real-time budget tracking against spending
- **Bill Payments** – Manage and pay bills from the dashboard
- **Loans** – Apply for loans, view loan products, track repayment schedules, and admin-side loan decisioning
- **Rewards** – Earn points on activity (e.g. payments)
- **Insights & Analytics** – Visual breakdown of spending and financial health
- **Admin Dashboard** – Manage users, loan products, and loan approvals

---

## 🛠️ Tech Stack

**Frontend**
- Angular 21
- Tailwind CSS
- Chart.js (data visualizations)
- RxJS

**Backend**
- Spring Boot 4 (Java 21)
- Spring Security + JWT (jjwt)
- Spring Data JPA (Hibernate)
- MySQL
- Springdoc OpenAPI (Swagger UI)
- Spring AOP & Caching

**Database**
- MySQL (see `database.sql` for schema)

---

## 📂 Project Structure

```
neobank360/
├── frontend/          # Angular application
│   └── src/app/
│       ├── auth/          # Login & registration
│       ├── dashboard/     # Main user dashboard
│       ├── transactions/  # Transaction history
│       ├── budget/        # Budget tracking
│       ├── bills/         # Bill payments
│       ├── loans/         # Loan application, repayment, history
│       ├── rewards/       # Rewards & points
│       ├── insights/      # Analytics & insights
│       ├── admin/         # Admin dashboard, loan products & decisions
│       ├── user/          # User profile
│       └── shared/        # Shared components (navbar, etc.)
│
├── backend/           # Spring Boot application
│   └── src/main/java/.../
│       ├── controller/    # REST controllers (Auth, Account, Transfer, Loan, Bill, Budget, Rewards, Insights, Analytics, Admin, etc.)
│       ├── service/
│       ├── repository/
│       └── model/
│
└── database.sql       # MySQL schema
```

---

## 🚀 Getting Started

### Prerequisites

- Node.js (v18+) & npm
- Java 21
- Maven
- MySQL

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/neobank360.git
cd neobank360
```

### 2. Database Setup

Create a MySQL database and import the schema:

```bash
mysql -u root -p -e "CREATE DATABASE neobank_db;"
mysql -u root -p neobank_db < database.sql
```

### 3. Backend Setup

```bash
cd backend
```

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/neobank_db
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

Run the backend:

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api`
Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### 4. Frontend Setup

```bash
cd frontend
npm install
npm start
```

The app will be available at `http://localhost:4200`

---

## 📸 Screenshots

### Landing Page
![Landing Page](./screenshots/homepage.png)

---

## 📄 Documentation

Additional project documentation (SRS, ER diagrams, QA reports, handover docs, and presentations) is available in the `/docs` folder of this repository.

---

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Ritik Shekhar Parida**
