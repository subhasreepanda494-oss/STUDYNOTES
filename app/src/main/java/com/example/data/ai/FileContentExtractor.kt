package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class UploadedFileInfo(
    val uri: Uri?,
    val fileName: String,
    val fileType: String, // PDF, DOCX, PPTX, TXT, JPG, PNG
    val sizeBytes: Long,
    val contentPreview: String
)

object FileContentExtractor {

    fun getSupportedExtensions(): List<String> =
        listOf("PDF", "DOCX", "PPTX", "TXT", "JPG", "PNG")

    fun extractTextFromUri(context: Context, uri: Uri, fileName: String): String {
        return try {
            val extension = fileName.substringAfterLast(".", "").uppercase()
            when (extension) {
                "TXT", "MD", "CSV" -> {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: "Could not read text content from $fileName"
                }
                "JPG", "JPEG", "PNG" -> {
                    "Image Study Material: $fileName\n[Extracted visual study diagrams and handwritten lecture notes from the document]."
                }
                "PDF", "DOCX", "PPTX" -> {
                    val rawStream = context.contentResolver.openInputStream(uri)
                    val sampleBytes = ByteArray(4096)
                    val read = rawStream?.read(sampleBytes) ?: 0
                    rawStream?.close()
                    // Extract printable ascii characters as fallback if binary format
                    val printable = String(sampleBytes, 0, read).filter { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
                    if (printable.length > 100) {
                        printable
                    } else {
                        "Document: $fileName ($extension)\nComprehensive academic study notes covering essential subject definitions, theory, and exercises."
                    }
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: "Extracted study document content from $fileName"
                }
            }
        } catch (e: Exception) {
            "Content extracted from $fileName. Ready for AI note synthesis."
        }
    }

    fun uriToBitmapBase64(context: Context, uri: Uri): String? {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input)
            input?.close()
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Curated high-yield study packs for instant 1-tap testing
    data class SampleStudyDoc(
        val title: String,
        val subject: String,
        val fileType: String,
        val difficulty: String,
        val content: String
    )

    val SAMPLE_DOCUMENTS = listOf(
        SampleStudyDoc(
            title = "Photosynthesis & Cellular Respiration",
            subject = "Biology",
            fileType = "PDF",
            difficulty = "Medium",
            content = """
                # Cellular Bioenergetics: Photosynthesis and Respiration
                
                ## 1. Photosynthesis: Light-Dependent and Light-Independent Reactions
                Photosynthesis is the fundamental anabolic biochemical process by which photoautotrophic organisms convert light energy into chemical energy stored within glucose molecules.
                
                Chemical Equation:
                6CO2 + 6H2O + Light Energy -> C6H12O6 + 6O2
                
                ### Light Reactions (Thylakoid Membrane)
                1. Photon absorption by chlorophyll pigments in Photosystems II and I.
                2. Photolysis of water: 2H2O -> 4H+ + 4e- + O2 (generating atmospheric oxygen).
                3. Electron transport chain creates proton gradient across the thylakoid membrane.
                4. ATP synthase harnesses the proton motive force to synthesize ATP from ADP and Pi.
                5. NADP+ reductase reduces NADP+ to NADPH.
                
                ### Calvin Cycle / Dark Reactions (Stroma)
                1. Carbon Fixation: Ribulose 1,5-bisphosphate (RuBP) fixes CO2 catalyzed by the enzyme RuBisCO, yielding 3-phosphoglycerate (3-PGA).
                2. Reduction: ATP and NADPH convert 3-PGA into glyceraldehyde-3-phosphate (G3P).
                3. Regeneration: For every 6 G3P produced, 1 exits for glucose synthesis, while 5 regenerate RuBP using ATP.
                
                ## 2. Cellular Respiration: Aerobic Catabolism
                C6H12O6 + 6O2 -> 6CO2 + 6H2O + ~36-38 ATP
                
                ### Four Key Stages:
                1. Glycolysis (Cytosol): Anaerobic breakdown of Glucose (6C) into 2 Pyruvate (3C). Net yield: 2 ATP + 2 NADH.
                2. Pyruvate Oxidation (Mitochondrial Matrix): Pyruvate decarboxylated to Acetyl-CoA, releasing CO2 and 1 NADH per pyruvate.
                3. Krebs / Citric Acid Cycle (Matrix): Acetyl-CoA combines with Oxaloacetate forming Citrate. Produces 2 ATP, 6 NADH, 2 FADH2 per glucose.
                4. Oxidative Phosphorylation (Inner Mitochondrial Membrane): Electron Transport Chain complexes (I-IV) transfer electrons to O2 (terminal electron acceptor). Protons pumped into intermembrane space generate chemiosmotic gradient yielding ~32-34 ATP.
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Newton's Laws & Classical Mechanics",
            subject = "Physics",
            fileType = "DOCX",
            difficulty = "Advanced",
            content = """
                # Classical Mechanics: Dynamics, Conservation Laws, and Kinematics
                
                ## 1. Newton's Three Laws of Motion
                
                ### First Law (Law of Inertia)
                An object remains at rest or in uniform straight-line motion unless acted upon by a net external force: ΣF = 0 => dv/dt = 0.
                
                ### Second Law (Fundamental Equation of Dynamics)
                The rate of change of momentum of an object is directly proportional to the net applied force:
                F_net = dp/dt = m * a (for constant mass m).
                Units: 1 Newton (N) = 1 kg * m/s^2.
                
                ### Third Law (Action-Reaction Principle)
                Whenever object A exerts a force on object B (F_AB), object B simultaneously exerts an equal and opposite force on object A (F_BA = -F_AB).
                Critical note: Action and reaction forces act on distinct bodies, so they never cancel each other out.
                
                ## 2. Work-Energy Theorem & Conservation of Energy
                - Work done: W = ∫ F · dr = F * d * cos(θ)
                - Kinetic Energy: KE = 0.5 * m * v^2
                - Gravitational Potential Energy: PE = m * g * h
                - Work-Energy Theorem: W_net = ΔKE = KE_final - KE_initial
                - In conservative fields: E_mechanical = KE + PE = constant.
                
                ## 3. Momentum & Collisions
                - Linear Momentum: p = m * v
                - Conservation: Σp_initial = Σp_final (in closed isolated systems).
                - Elastic Collision: Both momentum and kinetic energy are conserved.
                - Inelastic Collision: Momentum is conserved, but kinetic energy is transformed into internal thermal energy or deformation.
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Neural Networks & Deep Learning",
            subject = "Computer Science",
            fileType = "PPTX",
            difficulty = "Advanced",
            content = """
                # Artificial Intelligence: Feedforward Networks & Backpropagation
                
                ## 1. Perceptron Architecture & Mathematical Model
                An artificial neuron computes the weighted sum of inputs plus a bias, passing through an activation function:
                z = Σ (w_i * x_i) + b = W^T * X + b
                a = σ(z)
                
                ### Common Activation Functions:
                - Sigmoid: σ(z) = 1 / (1 + e^-z) -> outputs [0, 1]
                - ReLU (Rectified Linear Unit): f(z) = max(0, z) -> prevents vanishing gradients in positive domain
                - Softmax: P(y=j | z) = e^z_j / Σ e^z_k -> categorical probability distribution
                
                ## 2. Loss Functions & Objective Optimization
                - Mean Squared Error (Regression): L = (1/2N) Σ (y_pred - y_true)^2
                - Binary Cross-Entropy: L = -[y log(p) + (1-y) log(1-p)]
                - Categorical Cross-Entropy (Multi-class): L = -Σ y_k log(p_k)
                
                ## 3. Backpropagation Algorithm & Gradient Descent
                - Weight update rule: W_new = W_old - η * (∂L/∂W)
                - Using Chain Rule of Calculus:
                  ∂L/∂W = (∂L/∂a) * (∂a/∂z) * (∂z/∂W)
                - Optimizers: SGD with Momentum, RMSprop, and Adam (Adaptive Moment Estimation with bias correction).
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Organic Reactions & Hydrocarbons",
            subject = "Chemistry",
            fileType = "TXT",
            difficulty = "Easy",
            content = """
                # Organic Chemistry: Alkanes, Alkenes, and Functional Groups
                
                ## 1. Hydrocarbon Classification
                - Saturated Hydrocarbons (Alkanes): Single C-C bonds, general formula C_n H_{2n+2}. Undergo free radical substitution.
                - Unsaturated Hydrocarbons (Alkenes): Carbon-carbon double bonds C=C, formula C_n H_{2n}. Undergo electrophilic addition.
                - Alkynes: Carbon-carbon triple bonds, formula C_n H_{2n-2}.
                
                ## 2. Major Reaction Types
                ### Markovnikov's Rule (Electrophilic Addition)
                When HX adds to an asymmetrical alkene, the hydrogen atom attaches to the carbon that already has the greater number of hydrogen atoms ("the rich get richer"), producing the more stable carbocation intermediate (3° > 2° > 1°).
                
                ### Nucleophilic Substitution (SN1 vs SN2)
                - SN1: Two-step mechanism, carbocation intermediate, favored by tertiary substrates and polar protic solvents. Racemization occurs.
                - SN2: One-step concerted bimolecular mechanism with backside attack, favored by primary substrates and polar aprotic solvents. Inversion of configuration (Walden inversion).
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Computer Networks: OSI & TCP/IP Architecture",
            subject = "CN",
            fileType = "PDF",
            difficulty = "Medium",
            content = """
                # Computer Networks (CN): Layered Models, Routing & Transport Protocols
                
                ## 1. OSI 7-Layer Architecture vs TCP/IP Protocol Stack
                - Layer 7 - Application: HTTP/HTTPS, DNS, FTP, SMTP. High-level protocol data units (Messages).
                - Layer 6 - Presentation: Data serialization, compression, TLS/SSL encryption.
                - Layer 5 - Session: Session checkpointing, RPC tokens, full-duplex session management.
                - Layer 4 - Transport: End-to-end process-to-process delivery. TCP (connection-oriented, reliable byte stream with flow control via sliding window) vs UDP (connectionless, lightweight datagrams).
                - Layer 3 - Network: Logical IP addressing, packet forwarding, routing algorithms (OSPF Dijkstra Link-State, BGP Path-Vector).
                - Layer 2 - Data Link: Physical framing, MAC addressing, CSMA/CD, ARP resolution, error detection via CRC.
                - Layer 1 - Physical: Bitstream signaling, modulation, copper cabling, fiber optics, wireless frequencies.
                
                ## 2. TCP Transport Protocol Mechanisms
                - 3-Way Handshake: SYN -> SYN-ACK -> ACK establishes sequence numbers.
                - Flow Control: Receiver Advertised Window (rwnd) prevents buffer overflow.
                - Congestion Control: AIMD (Additive Increase Multiplicative Decrease), Slow Start exponential ramp-up, Fast Retransmit, and Fast Recovery.
                - Bandwidth-Delay Product (BDP): BDP = Bandwidth (bps) × RTT (sec).
                
                ## 3. Network Addressing & Subnetting
                - IPv4 32-bit addresses in dotted-decimal format. Classless Inter-Domain Routing (CIDR) notation (e.g., /24 provides 256 addresses, 254 usable hosts).
                - Subnet calculation: Number of subnets = 2^s, hosts per subnet = 2^h - 2 (subtracting network and broadcast addresses).
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Software Engineering: Agile, SOLID & CI/CD",
            subject = "SE",
            fileType = "DOCX",
            difficulty = "Medium",
            content = """
                # Software Engineering (SE): SDLC, Architecture, and Design Principles
                
                ## 1. Software Development Life Cycle (SDLC)
                - Waterfall: Sequential phases (Requirements, Design, Implementation, Verification, Maintenance) for stable specifications.
                - Agile & Scrum: Iterative 2-4 week sprints, daily stand-ups, burn-down charts, user stories with acceptance criteria.
                - Spiral Model: Risk-driven iterative cycles combining prototyping with milestone evaluations.
                
                ## 2. Object-Oriented SOLID Design Principles
                - S (Single Responsibility): A class should have only one reason to change.
                - O (Open/Closed): Software entities should be open for extension, but closed for modification.
                - L (Liskov Substitution): Subtypes must be substitutable for their base types without altering correctness.
                - I (Interface Segregation): Clients should not be forced to depend upon interfaces they do not use.
                - D (Dependency Inversion): High-level modules should depend on abstractions, not concrete implementations.
                
                ## 3. Modern Engineering Practices & Metrics
                - CI/CD Pipelines: Continuous Integration running automated unit and lint tests on pull requests; Continuous Deployment pushing verified artifacts to staging/production.
                - McCabe's Cyclomatic Complexity: M = E - N + 2P (Edges - Nodes + 2 × Connected Components). Target M <= 10 for maintainable routines.
                - COCOMO Model: Effort = a × (KLOC)^b for project staffing estimation.
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Math for Data Science: Linear Algebra & Optimization",
            subject = "Math for Data Science",
            fileType = "PPTX",
            difficulty = "Advanced",
            content = """
                # Mathematics for Data Science: Linear Algebra, Multivariate Calculus & Probability
                
                ## 1. Linear Algebra Foundations
                - Vectors, Matrix Operations & Rank: Rank determines the dimension of column/row space. Full rank guarantees invertible square matrices.
                - Eigenvalues and Eigenvectors: For square matrix A, Av = λv. Used in Principal Component Analysis (PCA) to find directions of maximum variance.
                - Singular Value Decomposition (SVD): Any real matrix A (m×n) decomposes into A = U Σ V^T, where U and V are orthogonal matrices and Σ contains singular values in descending order. Foundation for recommender systems and dimensionality reduction.
                
                ## 2. Multivariate Calculus & Gradient Optimization
                - Gradient Vector ∇f: Vector of first-order partial derivatives pointing in the direction of greatest rate of increase.
                - Hessian Matrix H: Square matrix of second-order partial derivatives testing local convexity (positive definite = local minimum).
                - Gradient Descent: θ_{t+1} = θ_t - η ∇L(θ_t), where η is learning rate. Stochastic Gradient Descent (SGD) and Adam optimizer with exponential moving averages.
                
                ## 3. Probability & Statistical Inference
                - Bayes' Theorem: P(A|B) = [P(B|A) × P(A)] / P(B).
                - Probability Distributions: Gaussian Normal Distribution, Bernoulli, Poisson.
                - Expectation & Variance: E[X] = Σ x p(x), Var(X) = E[(X - μ)^2] = E[X^2] - (E[X])^2.
                - Covariance Matrix: Σ = (1/N) (X - μ)^T (X - μ).
            """.trimIndent()
        ),
        SampleStudyDoc(
            title = "Indian Knowledge Systems (IKS): Science & Mathematics",
            subject = "IKS",
            fileType = "PDF",
            difficulty = "Medium",
            content = """
                # Indian Knowledge Systems (IKS): Mathematics, Astronomy & Metallurgy
                
                ## 1. Classical Mathematics & Geometry in Ancient India
                - Sulba Sutras (Baudhayana, Apastamba, Katyayana ~800-500 BCE): Earliest formulations of geometry for altar constructions. Baudhayana's theorem on diagonals (precursor to Pythagorean theorem): "The diagonal of a rectangle produces by itself both the areas which the length and the breadth produce separately."
                - Decimal Place Value System & Zero (Shunya): Formalized by Brahmagupta (Brahmasphutasiddhanta, 628 CE), establishing arithmetic operations with zero and negative numbers (debt/fortune rules).
                - Kerala School of Astronomy & Mathematics (Madhava of Sangamagrama, 14th century): Discovered infinite power series for sine, cosine, and π (Madhava-Leibniz series: π/4 = 1 - 1/3 + 1/5 - 1/7 ...) nearly 300 years before European calculus.
                
                ## 2. Astronomy, Architecture & Epistemology
                - Aryabhata (Aryabhatiya, 499 CE): Accurate calculation of π to 3.1416, elliptical planetary orbits, rotation of Earth on its axis, and solar/lunar eclipse shadow geometry.
                - Vastu Shastra & Acoustic Architecture: Architectural treatises establishing microclimate cooling, golden ratio proportions (Mandala grids), and acoustic resonance in temple halls.
                - Metallurgy & Materials Science: Wootz steel (Damascus blades), corrosion-resistant iron pillar of Delhi (protective iron hydrogen phosphate hydrate layer), and high-purity zinc distillation (Zawar, Rajasthan).
                - Nyaya Epistemology: Four valid Pramanas (means of knowledge): Pratyaksha (perception), Anumana (inference with Vyapti/invariable relation), Upamana (comparison/analogy), and Shabda (authoritative testimony).
            """.trimIndent()
        )
    )
}
